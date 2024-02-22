package eu.ill.visa.web.managed;

import com.google.inject.Inject;
import eu.ill.visa.vdi.WebsocketApplication;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ManagedWebsocketServer implements Managed {

    private final WebsocketApplication application;
    private final Logger logger = LoggerFactory.getLogger(ManagedWebsocketServer.class);

    @Inject
    public ManagedWebsocketServer(final WebsocketApplication application) {
        this.application = application;
    }

    @Override
    public void start() throws Exception {
        try {
            logger.info("Starting websocket server");
            this.application.startServer();

        } catch (Exception e) {
            logger.error("Error creating websocket Application: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping websocket server");
        this.application.stopServer();
    }

}
