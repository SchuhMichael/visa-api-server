package eu.ill.visa.vdi;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import eu.ill.visa.business.services.InstanceActivityService;
import eu.ill.visa.business.services.InstanceService;
import eu.ill.visa.business.services.InstanceSessionService;
import eu.ill.visa.vdi.endpoints.GuacamoleEndpoint;
import eu.ill.visa.vdi.services.DesktopConnectionService;
import eu.ill.visa.vdi.services.RoleService;
import eu.ill.visa.vdi.services.TokenAuthenticatorService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.websocket.server.ServerEndpointConfig;

@Singleton
public class WebsocketApplication {

    private final DesktopConnectionService desktopConnectionService;
    private final InstanceService instanceService;
    private final InstanceActivityService instanceActivityService;
    private final InstanceSessionService instanceSessionService;
    private final RoleService roleService;
    private final TokenAuthenticatorService authenticator;
    private Server server;

    @Inject
    public WebsocketApplication(final DesktopConnectionService desktopConnectionService,
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

    public void startServer() {
        this.server = new Server();
        ServerConnector connector = new ServerConnector(this.server);
        connector.setPort(8085);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        try {
            WebSocketServerContainerInitializer.configure(context,
                    (servletContext, wsContainer) -> {
                        wsContainer.setDefaultMaxTextMessageBufferSize(65535);

                        // Add WebSocket endpoint to javax.websocket layer
                        wsContainer.addEndpoint(ServerEndpointConfig.Builder
                                .create(GuacamoleEndpoint.class, "/ws/vdi")
                                .configurator(new GuacamoleEndpoint.Configurator(this.desktopConnectionService, this.instanceService, this.instanceActivityService, this.instanceSessionService, this.roleService, this.authenticator))
                                .build());
                    });

            server.start();

        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    public void stopServer() {
        try {
            this.server.stop();
            this.server.join();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
