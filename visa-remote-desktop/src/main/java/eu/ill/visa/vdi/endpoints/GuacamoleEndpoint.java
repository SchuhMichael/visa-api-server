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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@ServerEndpoint(value = "/socket", subprotocols = {"guacamole", "webx"})
public class GuacamoleEndpoint {
    private final static String CONNECTION_ID_PARAMETER = "connectionId";

    private static final Logger logger = LoggerFactory.getLogger(GuacamoleEndpoint.class);

    private final DesktopConnectionService desktopConnectionService;
    private final InstanceService instanceService;
    private final InstanceActivityService instanceActivityService;
    private final InstanceSessionService instanceSessionService;
    private final RoleService roleService;
    private final TokenAuthenticatorService authenticator;

    private Map<String, String> sessionConnectionIds = new HashMap<>();

    public GuacamoleEndpoint(final DesktopConnectionService desktopConnectionService,
                             final InstanceService instanceService,
                             final InstanceActivityService instanceActivityService,
                             final InstanceSessionService instanceSessionService,
                             final RoleService roleService,
                             final TokenAuthenticatorService authenticator) {
        this.desktopConnectionService = desktopConnectionService;
        this.instanceService = instanceService;
        this.instanceActivityService = instanceActivityService;
        this.instanceSessionService = instanceSessionService;
        this.roleService = roleService;
        this.authenticator = authenticator;
        System.out.println("Creating GuacamoleEndpoint");
    }

    @OnOpen
    public void onConnect(Session session) {
        logger.info("Initialising websocket client with session id: {}", session.getId());

        final Map<String, List<String>> data = session.getRequestParameterMap();
        final String connectionId = data.get(CONNECTION_ID_PARAMETER).get(0);
        try {

            final InstanceAuthenticationToken token = authenticator.authenticate(session);

            final User user = token.getUser();
            final Instance instance = token.getInstance();
            final Role role = roleService.getRole(instance, user);

            this.desktopConnectionService.startConnectionThread(connectionId, session, instance, user, role);

            this.sessionConnectionIds.put(session.getId(), connectionId);

        } catch (OwnerNotConnectedException exception) {
//            session.getBasicRemote().sendText(OWNER_AWAY_EVENT);
            logger.warn("OwnerNotConnected for connection {} so disconnecting", connectionId);
            this.closeConnection(session);

        } catch (UnauthorizedException exception) {
            logger.warn("Unauthorised connection for connection {} so disconnecting: {}", connectionId, exception.getMessage());
//            client.sendEvent(ACCESS_DENIED);
            this.closeConnection(session);

        } catch (InvalidTokenException exception) {
            logger.warn("InvalidTokenException for connection {} so disconnecting: {}", connectionId, exception.getMessage());
            this.closeConnection(session);

        } catch (ConnectionException exception) {
            logger.warn("ConnectionException for connection {} so disconnecting: {}", connectionId, exception.getMessage());
            this.closeConnection(session);
        }
    }

    @OnClose
    public void onDisconnect(CloseReason reason) {
        System.out.println("on disconnect " + reason);
    }

    @OnMessage
    public void onMessage(Session session, String data) throws IOException {
        String connectionId = this.sessionConnectionIds.get(session.getId());
        final DesktopConnection connection = this.desktopConnectionService.getDesktopConnection(connectionId);
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

                final InstanceSessionMember instanceSessionMember = this.instanceSessionService.getSessionMemberByConnectionId(connectionId);
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
        private final InstanceService instanceService;
        private final InstanceActivityService instanceActivityService;
        private final InstanceSessionService instanceSessionService;
        private final RoleService roleService;
        private final TokenAuthenticatorService authenticator;

        public Configurator(final DesktopConnectionService desktopConnectionService,
                            final InstanceService instanceService,
                            final InstanceActivityService instanceActivityService,
                            final InstanceSessionService instanceSessionService,
                            final RoleService roleService,
                            final TokenAuthenticatorService authenticator) {
            this.desktopConnectionService = desktopConnectionService;
            this.instanceService = instanceService;
            this.instanceActivityService = instanceActivityService;
            this.instanceSessionService = instanceSessionService;
            this.roleService = roleService;
            this.authenticator = authenticator;
        }

        public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
            return (T)new GuacamoleEndpoint(this.desktopConnectionService, this.instanceService, this.instanceActivityService, this.instanceSessionService, this.roleService, this.authenticator);
        }
    }
}
