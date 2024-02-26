package eu.ill.visa.vdi.services;

import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.store.StoreFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import eu.ill.visa.business.services.InstanceExpirationService;
import eu.ill.visa.business.services.InstanceSessionService;
import eu.ill.visa.core.domain.Instance;
import eu.ill.visa.core.domain.InstanceSession;
import eu.ill.visa.core.domain.InstanceSessionMember;
import eu.ill.visa.core.domain.User;
import eu.ill.visa.vdi.VirtualDesktopConfiguration;
import eu.ill.visa.vdi.concurrency.ConnectionThread;
import eu.ill.visa.vdi.domain.Role;
import eu.ill.visa.vdi.events.*;
import eu.ill.visa.vdi.exceptions.ConnectionException;
import eu.ill.visa.vdi.exceptions.OwnerNotConnectedException;
import eu.ill.visa.vdi.exceptions.UnauthorizedException;
import eu.ill.visa.vdi.models.ConnectedUser;
import eu.ill.visa.vdi.models.DesktopConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.util.*;
import java.util.stream.Collectors;

import static eu.ill.visa.vdi.events.Event.ACCESS_REVOKED_EVENT;
import static eu.ill.visa.vdi.events.Event.OWNER_AWAY_EVENT;

@Singleton
public class DesktopConnectionService {
    private final static String PROTOCOL_PARAMETER = "protocol";
    private final static Logger logger = LoggerFactory.getLogger(DesktopConnectionService.class);

    private final InstanceSessionService instanceSessionService;
    private final GuacamoleDesktopService guacamoleDesktopService;
    private final WebXDesktopService webXDesktopService;
    private final InstanceExpirationService instanceExpirationService;
    private final VirtualDesktopConfiguration virtualDesktopConfiguration;

    private final List<DesktopConnection> desktopConnections = new ArrayList<>();
    private final StoreFactory storeFactory;

    @Inject
    public DesktopConnectionService(final InstanceSessionService instanceSessionService,
                                    final GuacamoleDesktopService guacamoleDesktopService,
                                    final WebXDesktopService webXDesktopService,
                                    final InstanceExpirationService instanceExpirationService,
                                    final VirtualDesktopConfiguration virtualDesktopConfiguration,
                                    final StoreFactory storeFactory) {
        this.instanceSessionService = instanceSessionService;
        this.guacamoleDesktopService = guacamoleDesktopService;
        this.webXDesktopService = webXDesktopService;
        this.instanceExpirationService = instanceExpirationService;
        this.virtualDesktopConfiguration = virtualDesktopConfiguration;
        this.storeFactory = storeFactory;
    }

    public void broadcast(final SocketIOClient client, final Event ...events) {
        final DesktopConnection connection = this.getDesktopConnection(client.getSessionId().toString());
        this.broadcast(client, connection.getRoomId(), events);
    }

    public void broadcast(final SocketIOClient client, String roomId, final Event ...events) {
        final SocketIONamespace namespace = client.getNamespace();
        final BroadcastOperations operations = namespace.getRoomOperations(roomId);

//        final RoomBroadcastOperations roomBroadcastOperations = new RoomBroadcastOperations(operations.getClients(), this.storeFactory, roomId);
//
//        for (Event event : events) {
//            event.broadcast(client, roomBroadcastOperations);
//        }
    }

    public DesktopConnection createDesktopConnection(final String connectionId, final SocketIOClient client, final Instance instance, final User user, final Role role) throws OwnerNotConnectedException, UnauthorizedException, ConnectionException {
        if (role == Role.NONE) {
            throw new UnauthorizedException("User " + user.getFullName() + " is unauthorised to access the instance " + instance.getId());
        }

        ConnectedUser connectedUser = new ConnectedUser(user.getId(), user.getFullName(), role);
        DesktopConnection desktopConnection = new DesktopConnection(connectionId, client,  instance.getId(), instance.getLastSeenAt(), connectedUser, instance.getId().toString());
        this.desktopConnections.add(desktopConnection);

        client.joinRoom(desktopConnection.getRoomId());

        return desktopConnection;
    }

    public void startConnectionThread(final String connectionId, final Session session, final Instance instance, final User user, final Role role) throws OwnerNotConnectedException, UnauthorizedException, ConnectionException {
        DesktopConnection desktopConnection = this.getDesktopConnection(connectionId);
        if (desktopConnection == null) {
            throw new UnauthorizedException("DesktopConnection has not been initialised with Id " + connectionId);

        } else if (!desktopConnection.getConnectedUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("DesktopConnection has been established by a different user: " + connectionId);

        } else if (!desktopConnection.getInstanceId().equals(instance.getId())) {
            throw new UnauthorizedException("DesktopConnection has been established for a different instance: " + connectionId);
        }

        final Map<String, List<String>> data = session.getRequestParameterMap();
        final String protocol = data.get(PROTOCOL_PARAMETER).get(0);

        boolean isWebX = protocol != null && protocol.equals("webx");
        final ConnectionThread connectionThread;
        if (isWebX) {
            logger.info("User {} creating WebX desktop connection to instance {}", (user.getFullName() + "(" + role.toString() + ")"), instance.getId());
            connectionThread = webXDesktopService.connect(session, instance, user, role);
        } else {
            logger.info("User {} creating Guacamole desktop connection to instance {}", (user.getFullName() + "(" + role.toString() + ")"), instance.getId());
            connectionThread = guacamoleDesktopService.connect(session, instance, user, role);
        }

        desktopConnection.setConnectionThread(connectionThread);


        InstanceSession instanceSession = this.instanceSessionService.getByInstance(instance);
        boolean unlockRoom = virtualDesktopConfiguration.getOwnerDisconnectionPolicy().equals(VirtualDesktopConfiguration.OWNER_DISCONNECTION_POLICY_LOCK_ROOM)
            && role.equals(Role.OWNER)
            && !this.isOwnerConnected(instance);

        // Update the connected clients of the session
        this.instanceSessionService.addInstanceSessionMember(instanceSession, connectionId, user, role.toString());

        // Remove instance from instance_expiration table if it is there due to inactivity
        this.instanceExpirationService.onInstanceActivated(instanceSession.getInstance());

        if (unlockRoom) {
            // Unlock room for all clients
            this.unlockRoom(connectionId, desktopConnection.getRoomId(), instance);

        } else {
            this.broadcast(desktopConnection.getClient(),
                new UserConnectedEvent(this.getConnectedUser(connectionId)),
                new UsersConnectedEvent(instance, this.getConnectedUsers(instance, false))
            );
        }

    }

    public DesktopConnection getDesktopConnection(final String connectionId) {
        return this.desktopConnections.stream()
            .filter(desktopConnection -> desktopConnection.getConnectionId().equals(connectionId))
            .findFirst()
            .orElse(null);
    }

    public DesktopConnection getDesktopConnectionByClient(final SocketIOClient client) {
        return this.desktopConnections.stream()
            .filter(desktopConnection -> desktopConnection.getClient().getSessionId().equals(client.getSessionId()))
            .findFirst()
            .orElse(null);
    }

    public void removeDesktopConnection(final String connectionId) {
        DesktopConnection desktopConnection = this.getDesktopConnection(connectionId);
        if (desktopConnection != null) {
            this.desktopConnections.remove(desktopConnection);
        }
    }

    public ConnectedUser getConnectedUser(final String connectionId) {
        final DesktopConnection desktopConnection = this.getDesktopConnection(connectionId);
        return desktopConnection.getConnectedUser();
    }

    public List<ConnectedUser> getConnectedUsers(Instance instance, boolean isRoomLocked) {
        List<InstanceSessionMember> instanceSessionMembers = this.instanceSessionService.getAllSessionMembers(instance);
        logger.info("Instance {} has {} connected users", instance.getId(), instanceSessionMembers.size());
        List<ConnectedUser> connectedUsers = instanceSessionMembers.stream().map(instanceSessionMember -> {
            User user = instanceSessionMember.getUser();
            Role role = Role.valueOf(instanceSessionMember.getRole());
            if (isRoomLocked && role.equals(Role.USER)) {
                role= Role.GUEST;
            }
            return new ConnectedUser(user.getId(), user.getFullName(), role);
        }).collect(Collectors.toUnmodifiableList());

        return connectedUsers;
    }

    public boolean isOwnerConnected(final Instance instance) {
        List<InstanceSessionMember> instanceSessionMembers = this.instanceSessionService.getAllSessionMembers(instance);
        for (final InstanceSessionMember instanceSessionMember : instanceSessionMembers) {
            String role = instanceSessionMember.getRole();
            if (role.equals("OWNER")) {
                return true;
            }
        }

        return false;
    }

    public void disconnectAllRoomClients(final String connectionId, final String room) {
        DesktopConnection desktopConnection = this.getDesktopConnection(connectionId);
        if (desktopConnection != null) {
            SocketIOClient client = desktopConnection.getClient();

            final SocketIONamespace namespace = client.getNamespace();

            final BroadcastOperations operations = namespace.getRoomOperations(room);
            final Collection<SocketIOClient> clients = operations.getClients();
            this.broadcast(client, new RoomClosedEvent());

            for (final SocketIOClient aClient : clients) {
                aClient.sendEvent(OWNER_AWAY_EVENT);
                aClient.disconnect();
            }
        }
    }

    public void lockRoom(final String connectionId, final String room, Instance instance) {
        DesktopConnection desktopConnection = this.getDesktopConnection(connectionId);
        if (desktopConnection != null) {
            SocketIOClient client = desktopConnection.getClient();

            final SocketIONamespace namespace = client.getNamespace();

            final BroadcastOperations operations = namespace.getRoomOperations(room);
            final Collection<SocketIOClient> clients = operations.getClients();

            // broadcast room closed and current connected users
            this.broadcast(client,
                new RoomLockedEvent(instance),
                new UsersConnectedEvent(instance, this.getConnectedUsers(instance, true))
            );

            for (final SocketIOClient aClient : clients) {
                DesktopConnection connection = this.getDesktopConnectionByClient(aClient);
                if (connection != null) {
                    connection.setRoomLocked(true);
                }
            }
        }
    }

    public void unlockRoom(final String connectionId, final String room, Instance instance) {
        DesktopConnection desktopConnection = this.getDesktopConnection(connectionId);
        if (desktopConnection != null) {
            SocketIOClient client = desktopConnection.getClient();
            final SocketIONamespace namespace = client.getNamespace();

            final BroadcastOperations operations = namespace.getRoomOperations(room);
            final Collection<SocketIOClient> clients = operations.getClients();

            // broadcast room closed and current connected users
            this.broadcast(client,
                new RoomUnlockedEvent(instance),
                new UsersConnectedEvent(instance, this.getConnectedUsers(instance, false))
            );

            for (final SocketIOClient aClient : clients) {
                DesktopConnection connection = this.getDesktopConnectionByClient(aClient);
                connection.setRoomLocked(false);
            }
        }
    }

    public boolean disconnectClient(SocketIOClient owner, String room, UUID clientSessionId) {
        final SocketIONamespace namespace = owner.getNamespace();

        final BroadcastOperations operations = namespace.getRoomOperations(room);
        final Collection<SocketIOClient> clients = operations.getClients();

        Optional<SocketIOClient> clientOptional = clients.stream()
            .filter(aClient -> aClient.getSessionId().equals(clientSessionId))
            .findFirst();

        if (clientOptional.isPresent()) {
            logger.info("Disconnecting client {} from room {}", clientSessionId, room);
            SocketIOClient aClient = clientOptional.get();
            aClient.sendEvent(ACCESS_REVOKED_EVENT);
            aClient.disconnect();

            return true;

        } else {
            return false;
        }
    }
}
