package eu.ill.visa.vdi.endpoints;

import eu.ill.visa.business.services.InstanceActivityService;
import eu.ill.visa.business.services.InstanceService;
import eu.ill.visa.business.services.InstanceSessionService;
import eu.ill.visa.core.domain.Instance;
import eu.ill.visa.core.domain.InstanceAuthenticationToken;
import eu.ill.visa.core.domain.InstanceSessionMember;
import eu.ill.visa.core.domain.User;
import eu.ill.visa.core.domain.enumerations.InstanceActivityType;
import eu.ill.visa.vdi.concurrency.ConnectionThread;
import eu.ill.visa.vdi.domain.Role;
import eu.ill.visa.vdi.exceptions.ConnectionException;
import eu.ill.visa.vdi.exceptions.InvalidTokenException;
import eu.ill.visa.vdi.exceptions.OwnerNotConnectedException;
import eu.ill.visa.vdi.exceptions.UnauthorizedException;
import eu.ill.visa.vdi.models.DesktopConnection;
import eu.ill.visa.vdi.services.DesktopAccessService;
import eu.ill.visa.vdi.services.DesktopConnectionService;
import eu.ill.visa.vdi.services.RoleService;
import eu.ill.visa.vdi.services.TokenAuthenticatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.util.Date;

import static eu.ill.visa.vdi.domain.Role.SUPPORT;
import static java.lang.String.format;

@ServerEndpoint(value = "/socket", subprotocols = "guacamole")
public class GuacamoleEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(GuacamoleEndpoint.class);

    private final DesktopConnectionService desktopConnectionService;
    private final DesktopAccessService desktopAccessService;
    private final InstanceService instanceService;
    private final InstanceActivityService instanceActivityService;
    private final InstanceSessionService instanceSessionService;
    private final RoleService roleService;
    private final TokenAuthenticatorService authenticator;

    public GuacamoleEndpoint(final DesktopConnectionService desktopConnectionService,
                             final DesktopAccessService desktopAccessService,
                             final InstanceService instanceService,
                             final InstanceActivityService instanceActivityService,
                             final InstanceSessionService instanceSessionService,
                             final RoleService roleService,
                             final TokenAuthenticatorService authenticator) {
        this.desktopConnectionService = desktopConnectionService;
        this.desktopAccessService = desktopAccessService;
        this.instanceService = instanceService;
        this.instanceActivityService = instanceActivityService;
        this.instanceSessionService = instanceSessionService;
        this.roleService = roleService;
        this.authenticator = authenticator;
    }

    @OnOpen
    public void onConnect(Session session) {
        try {
            logger.info("Initialising websocket client with session id: {}", session.getId());

            final InstanceAuthenticationToken token = authenticator.authenticate(session);

            final User user = token.getUser();
            final Instance instance = token.getInstance();
            final Role role = roleService.getRole(instance, user);

            if (instance.getUsername() == null) {
                logger.warn("No username is associated with the instance {}: the owner has never connected. Disconnecting user {}", instance.getId(), user);
                throw new OwnerNotConnectedException();

            } else {
                if (role.equals(SUPPORT)) {
//                    // See if user can connect even if owner is away
//                    if (this.instanceSessionService.canConnectWhileOwnerAway(instance, user)) {
//                        this.desktopConnectionService.createDesktopConnection(session, instance, user, role);
//
//                    } else {
//                        if (this.desktopConnectionService.isOwnerConnected(instance)) {
//                            // Start process of requesting access from the owner
//                            this.desktopAccessService.initiateAccess(client, user, instance);
//
//                        } else {
//                            throw new OwnerNotConnectedException();
//                        }
//                    }

                } else {
                    this.desktopConnectionService.createDesktopConnection(session, instance, user, role);
                }
            }

        } catch (OwnerNotConnectedException exception) {
//            session.getBasicRemote().sendText(OWNER_AWAY_EVENT);
            this.closeConnection(session);

        } catch (UnauthorizedException exception) {
            logger.warn(exception.getMessage());
//            client.sendEvent(ACCESS_DENIED);
            this.closeConnection(session);

        } catch (InvalidTokenException | ConnectionException exception) {
            logger.error(exception.getMessage());
            this.closeConnection(session);
        }
    }

    @OnClose
    public void onDisconnect(CloseReason reason) {
        System.out.println("on disconnect " + reason);
    }

    @OnMessage
    public void onMessage(Session session, String data) throws IOException {
        final DesktopConnection connection = this.desktopConnectionService.getDesktopConnection(session.getId());
        if (connection == null) {
            return;
        }

        InstanceActivityType controlActivityType = this.getControlActivityType(data);
        if (controlActivityType != null) {
            connection.setInstanceActivity(controlActivityType);
        }

        Role role = connection.getConnectedUser().getRole();
        if (controlActivityType == null || role.equals(Role.OWNER) || role.equals(Role.SUPPORT) || (role.equals(Role.USER) && !connection.isRoomLocked())) {
            this.writeData(connection.getConnectionThread(), data);
        }

        // Update last seen time of instance if more than one minute
        final Date lastSeenDate = connection.getLastSeenAt();
        final Date currentDate = new Date();
        if (lastSeenDate == null || (currentDate.getTime() - lastSeenDate.getTime() > 5 * 1000)) {
            final Long instanceId = connection.getInstanceId();
            final Instance instance = instanceService.getById(instanceId);
            if (instance == null) {
                logger.warn(format("Instance not found %d for connected user %s with role %s", instanceId, connection.getConnectedUser().getFullName(), role));
                this.closeConnection(session);

            } else {
                instance.updateLastSeenAt();

                if (lastSeenDate == null || lastSeenDate.getTime() <= connection.getLastInteractionAt().getTime()) {
                    instance.setLastInteractionAt(connection.getLastInteractionAt());
                }

                instanceService.save(instance);

                final InstanceSessionMember instanceSessionMember = this.instanceSessionService.getSessionMemberBySessionId(session.getId());
                if (instanceSessionMember == null) {
                    logger.warn(format("Instance session member not found for instance %d", instanceId));
                } else {
                    instanceSessionMember.updateLastSeenAt();
                    instanceSessionMember.setLastInteractionAt(connection.getLastInteractionAt());
                    instanceSessionService.saveInstanceSessionMember(instanceSessionMember);

                    InstanceActivityType instanceActivityType = connection.getInstanceActivity();
                    if (instanceActivityType != null) {
                        this.instanceActivityService.create(instanceSessionMember.getUser(), instance, instanceActivityType);
                        connection.resetInstanceActivity();
                    }
                }
                connection.setLastSeenAt(instance.getLastSeenAt());
            }
        }

    }

    private void closeConnection(Session session) {
        try {
            session.close();
        } catch (IOException exception) {
            logger.error("Failed to close websocket sesssion {}: {}", session.getId(), exception.getMessage());
        }
    }

    private InstanceActivityType getControlActivityType(String data) {
        int separatorPos = data.indexOf('.');
        int commandLength = Integer.parseInt(data.substring(0, separatorPos));
        String command = data.substring(separatorPos + 1, separatorPos + 1 + commandLength);

        if (command.equals("mouse")) {
            return InstanceActivityType.MOUSE;

        } else if (command.equals("key")){
            return InstanceActivityType.KEYBOARD;
        }

        return null;
    }

    protected void writeData(ConnectionThread connectionThread, String data) {
        connectionThread.writeCharData(data.toCharArray());
    }

    public static final class Configurator extends ServerEndpointConfig.Configurator {
        private final DesktopConnectionService desktopConnectionService;
        private final DesktopAccessService desktopAccessService;
        private final InstanceService instanceService;
        private final InstanceActivityService instanceActivityService;
        private final InstanceSessionService instanceSessionService;
        private final RoleService roleService;
        private final TokenAuthenticatorService authenticator;

        public Configurator(final DesktopConnectionService desktopConnectionService,
                            final DesktopAccessService desktopAccessService,
                            final InstanceService instanceService,
                            final InstanceActivityService instanceActivityService,
                            final InstanceSessionService instanceSessionService,
                            final RoleService roleService,
                            final TokenAuthenticatorService authenticator) {
            this.desktopConnectionService = desktopConnectionService;
            this.desktopAccessService = desktopAccessService;
            this.instanceService = instanceService;
            this.instanceActivityService = instanceActivityService;
            this.instanceSessionService = instanceSessionService;
            this.roleService = roleService;
            this.authenticator = authenticator;
        }

        public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
            return (T)new GuacamoleEndpoint(this.desktopConnectionService, this.desktopAccessService, this.instanceService, this.instanceActivityService, this.instanceSessionService, this.roleService, this.authenticator);
        }
    }
}
