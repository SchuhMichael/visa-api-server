package eu.ill.visa.vdi.business.services;

import eu.ill.visa.broker.MessageBroker;
import eu.ill.visa.business.services.InstanceService;
import eu.ill.visa.business.services.InstanceSessionService;
import eu.ill.visa.core.entity.Instance;
import eu.ill.visa.core.entity.InstanceMember;
import eu.ill.visa.core.entity.enumerations.InstanceMemberRole;
import eu.ill.visa.vdi.broker.AccessRequestCancellationMessage;
import eu.ill.visa.vdi.broker.AccessRequestMessage;
import eu.ill.visa.vdi.broker.AccessRequestResponseMessage;
import eu.ill.visa.vdi.domain.exceptions.ConnectionException;
import eu.ill.visa.vdi.domain.exceptions.OwnerNotConnectedException;
import eu.ill.visa.vdi.domain.exceptions.UnauthorizedException;
import eu.ill.visa.vdi.domain.models.*;
import eu.ill.visa.vdi.gateway.events.AccessCancellationEvent;
import eu.ill.visa.vdi.gateway.events.AccessRequestEvent;
import eu.ill.visa.vdi.gateway.events.AccessRequestResponseEvent;
import io.quarkus.runtime.Shutdown;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static eu.ill.visa.vdi.domain.models.SessionEvent.ACCESS_DENIED;

@Startup
@ApplicationScoped
public class DesktopAccessService {
    private static final Logger logger = LoggerFactory.getLogger(DesktopAccessService.class);

    private final DesktopSessionService desktopSessionService;
    private final InstanceService instanceService;
    private final InstanceSessionService instanceSessionService;

    private final MessageBroker messageBroker;

    private final List<DesktopCandidate> desktopCandidates = new ArrayList<>();

    private boolean keepingAlive = true;

    @Inject
    public DesktopAccessService(final DesktopSessionService desktopSessionService,
                                final InstanceService instanceService,
                                final InstanceSessionService instanceSessionService,
                                final jakarta.enterprise.inject.Instance<MessageBroker> messageBrokerInstance) {
        this.desktopSessionService = desktopSessionService;
        this.instanceService = instanceService;
        this.instanceSessionService = instanceSessionService;
        this.messageBroker = messageBrokerInstance.get();

        this.messageBroker.subscribe(AccessRequestMessage.class)
            .next((message) -> this.onAccessRequested(message.sessionId(), message.user(), message.requesterConnectionId()));
        this.messageBroker.subscribe(AccessRequestCancellationMessage.class)
            .next((message) -> this.onAccessRequestCancelled(message.sessionId(), message.user(), message.requesterConnectionId()));
        this.messageBroker.subscribe(AccessRequestResponseMessage.class)
            .next((message) -> this.onAccessRequestResponse(message.sessionId(), message.requesterConnectionId(), message.role()));

        Thread keepAliveThread = new Thread(() -> {
            while (this.keepingAlive) {
                try {
                    Thread.sleep(1000);
                    this.desktopCandidates.forEach(DesktopCandidate::keepAlive);
                } catch (InterruptedException ignored) {
                }
            }
        });
        keepAliveThread.start();
    }

    @Shutdown
    public void shutdown() {
        this.keepingAlive = false;
    }

    public void requestAccess(final SocketClient client, final Long sessionId, final PendingDesktopSessionMember pendingDesktopSessionMember, final NopSender nopSender) {
        // Create pending desktop connection
        DesktopCandidate desktopCandidate = this.addCandidate(client, sessionId, pendingDesktopSessionMember, nopSender);

        pendingDesktopSessionMember.eventChannel().sendEvent(SessionEvent.ACCESS_PENDING_EVENT);

        this.messageBroker.broadcast(new AccessRequestMessage(sessionId, pendingDesktopSessionMember.connectedUser(), desktopCandidate.client().token()));
    }

    public void cancelAccessRequest(final SocketClient client) {
        // Verify that access has been requested
        this.removeCandidate(client.token()).ifPresent(desktopCandidate -> {
            // A desktop request was in progress
            final PendingDesktopSessionMember pendingDesktopSessionMember = desktopCandidate.pendingDesktopSessionMember();
            this.messageBroker.broadcast(new AccessRequestCancellationMessage(desktopCandidate.sessionId(), pendingDesktopSessionMember.connectedUser(), client.token()));
        });
    }

    public void respondToAccessRequest(final Long sessionId, final String requesterConnectionId, final InstanceMemberRole role) {
        // Forward reply to broker
        this.messageBroker.broadcast(new AccessRequestResponseMessage(sessionId, requesterConnectionId, role));
    }

    private void onAccessRequested(final Long sessionId, final ConnectedUser user, final String requesterConnectionId) {
        // See if we have the owner of the instance
        this.desktopSessionService.findDesktopSession(sessionId).ifPresent(desktopSession -> {
            List<DesktopSessionMember> ownerSessionMembers = desktopSession.filterMembers(desktopSessionMember -> desktopSessionMember.getConnectedUser().getRole().equals(InstanceMemberRole.OWNER)).toList();
            if (!ownerSessionMembers.isEmpty()) {
                logger.info("Handling access request for instance {} from user {} with client id {}", desktopSession.getInstanceId(), user.getFullName(), requesterConnectionId);

                // Send request to owners
                ownerSessionMembers.forEach(ownerSessionMember -> {
                    ownerSessionMember.sendEvent(SessionEvent.ACCESS_REQUEST_EVENT, new AccessRequestEvent(sessionId, user, requesterConnectionId));
                });
            }
        });
    }

    private void onAccessRequestCancelled(final Long sessionId, final ConnectedUser user, final String requesterConnectionId) {
        this.desktopSessionService.findDesktopSession(sessionId).ifPresent(desktopSession -> {
            List<DesktopSessionMember> ownerSessionMembers = desktopSession.filterMembers(desktopSessionMember -> desktopSessionMember.getConnectedUser().getRole().equals(InstanceMemberRole.OWNER)).toList();
            if (!ownerSessionMembers.isEmpty()) {
                logger.info("Handling cancellation for instance {} from user {} with client id {}", desktopSession.getInstanceId(), user.getFullName(), requesterConnectionId);

                // Send cancellation to owners
                ownerSessionMembers.forEach(ownerSessionMember -> {
                    ownerSessionMember.sendEvent(SessionEvent.ACCESS_CANCELLATION_EVENT, new AccessCancellationEvent(user.getFullName(), requesterConnectionId));
                });
            }
        });
    }

    private void onAccessRequestResponse(final Long sessionId, final String requesterConnectionId, final InstanceMemberRole role) {
        this.removeCandidate(requesterConnectionId).ifPresent(desktopCandidate -> {
            final PendingDesktopSessionMember pendingDesktopSessionMember = desktopCandidate.pendingDesktopSessionMember();
            logger.info("Handling response ({}) of access request for instance {} from user {} with client id {}", role, pendingDesktopSessionMember.instanceId(), pendingDesktopSessionMember.connectedUser().getFullName(), requesterConnectionId);
            this.connectFromAccessReply(desktopCandidate, role);
        });

        // Send message to any other owners that request has been handled
        this.desktopSessionService.findDesktopSession(sessionId).ifPresent(desktopSession -> {
            desktopSession.filterMembers(desktopSessionMember -> desktopSessionMember.getConnectedUser().getRole().equals(InstanceMemberRole.OWNER)).forEach(ownerSessionMember -> {
                ownerSessionMember.sendEvent(SessionEvent.ACCESS_REPLY_EVENT, new AccessRequestResponseEvent(sessionId, requesterConnectionId, role.toString()));
            });
        });
    }

    private synchronized DesktopCandidate addCandidate(final SocketClient client, final Long sessionId, final PendingDesktopSessionMember pendingDesktopSessionMember, final NopSender nopSender) {
        DesktopCandidate desktopCandidate = new DesktopCandidate(client, sessionId, pendingDesktopSessionMember, nopSender);
        this.desktopCandidates.add(desktopCandidate);

        return desktopCandidate;
    }

    private synchronized Optional<DesktopCandidate> getCandidateById(String connectionId) {
        return this.desktopCandidates.stream()
            .filter(candidate -> candidate.client().token().equals(connectionId))
            .findAny();
    }

    private synchronized Optional<DesktopCandidate> removeCandidate(String connectionId) {
        return this.getCandidateById(connectionId).stream()
            .peek(this.desktopCandidates::remove)
            .findAny();
    }

    private void connectFromAccessReply(final DesktopCandidate candidate, final InstanceMemberRole replyRole) {

        final SocketClient client = candidate.client();
        if (client.isChannelOpen()) {
            final PendingDesktopSessionMember pendingDesktopSessionMember = candidate.pendingDesktopSessionMember();
            final ConnectedUser user = pendingDesktopSessionMember.connectedUser();
            final Long instanceId = pendingDesktopSessionMember.instanceId();
            final EventChannel eventChannel = pendingDesktopSessionMember.eventChannel();
            final Instance instance = this.instanceService.getFullById(instanceId);
            if (instance != null) {
                // Convert the support role to a normal user one if the owner of the instance is staff
                InstanceMemberRole role = this.convertAccessReplyRole(replyRole, instance, user);
                user.setRole(role);
                try {
                    this.desktopSessionService.createDesktopSessionMember(client, pendingDesktopSessionMember);

                    eventChannel.sendEvent(SessionEvent.ACCESS_GRANTED_EVENT, role);

                } catch (OwnerNotConnectedException exception) {
                    eventChannel.sendEvent(SessionEvent.OWNER_AWAY_EVENT);
                    client.disconnect();

                } catch (UnauthorizedException exception) {
                    logger.warn(exception.getMessage());
                    eventChannel.sendEvent(ACCESS_DENIED);
                    client.disconnect();

                } catch (ConnectionException exception) {
                    logger.error(exception.getMessage());
                    client.disconnect();
                }

            } else {
                logger.info("Instance {} no longer exists for access reply", instanceId);
            }

        } else {
            logger.info("Client {} is no longer waiting for an access reply", client.token());
        }
    }

    private InstanceMemberRole convertAccessReplyRole(InstanceMemberRole replyRole, Instance instance, ConnectedUser user) {
        if (replyRole.equals(InstanceMemberRole.SUPPORT)) {
            InstanceMember owner = instance.getOwner();
            boolean ownerIsExternalUser = !owner.getUser().hasRole(eu.ill.visa.core.entity.Role.STAFF_ROLE);
            if (ownerIsExternalUser) {
                // See if user has right to access instance when owner away (support role, otherwise user role)
                if (this.instanceSessionService.canConnectWhileOwnerAway(instance, user.getId())) {
                    // SUPPORT role if user can connect while owner away
                    return InstanceMemberRole.SUPPORT;

                } else {
                    // Standard USER role if user cannot connect if owner is away
                    return InstanceMemberRole.USER;
                }

            } else {
                // Standard user for staff
                return InstanceMemberRole.USER;
            }
        } else {
            return replyRole;
        }

    }
}
