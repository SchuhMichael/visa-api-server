package eu.ill.visa.vdi.gateway.subscribers;

import eu.ill.visa.vdi.business.services.DesktopSessionService;
import eu.ill.visa.vdi.domain.models.SocketClient;
import eu.ill.visa.vdi.gateway.dispatcher.ClientDisconnectSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientEventsDisconnectSubscriber implements ClientDisconnectSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(ClientEventsDisconnectSubscriber.class);

    private final DesktopSessionService desktopSessionService;

    public ClientEventsDisconnectSubscriber(final DesktopSessionService desktopSessionService) {
        this.desktopSessionService = desktopSessionService;
    }

    @Override
    public void onDisconnect(SocketClient socketClient) {
        // See if the desktop session is pending
        this.desktopSessionService.getPendingDesktopSessionMember(socketClient.token()).ifPresentOrElse(pendingDesktopSessionMember -> {
            logger.info("Event Connection disconnected by client {}: removing pending Session Member.", socketClient.token());
            this.desktopSessionService.removePendingDesktopSessionMember(pendingDesktopSessionMember);
        }, () -> {

            // If not pending then disable the event channel (client should try to reconnect event channel)
            this.desktopSessionService.findDesktopSessionMemberByToken(socketClient.token()).ifPresent(desktopSessionMember -> {
                logger.info("Event Connection disconnected by client {}: disabling event connection in session member: {}", socketClient.token(), desktopSessionMember);
                desktopSessionMember.setEventConnection(null);
            });
        });
    }
}

