package eu.ill.visa.vdi.listeners;

import com.corundumstudio.socketio.SocketIOClient;
import eu.ill.visa.core.domain.Instance;
import eu.ill.visa.vdi.events.Event;
import eu.ill.visa.vdi.models.ConnectedUser;
import eu.ill.visa.vdi.models.DesktopConnection;
import eu.ill.visa.vdi.services.DesktopConnectionService;

import java.util.List;
import java.util.UUID;

public class AbstractListener {

    protected final DesktopConnectionService desktopConnectionService;

    public AbstractListener(DesktopConnectionService desktopConnectionService) {
        this.desktopConnectionService = desktopConnectionService;
    }

    public void broadcast(final SocketIOClient client, final Event ...events) {
        this.desktopConnectionService.broadcast(client, events);
    }

    public DesktopConnection getDesktopConnectionByClient(final SocketIOClient client) {
        return this.desktopConnectionService.getDesktopConnectionByClient(client);
    }

    public void removeDesktopConnection(final UUID connectionId) {
        this.desktopConnectionService.removeDesktopConnection(connectionId);
    }

    public ConnectedUser getConnectedUser(final UUID connectionId) {
        return this.desktopConnectionService.getConnectedUser(connectionId);
    }

    public List<ConnectedUser> getConnectedUsers(Instance instance, boolean isRoomLocked) {
        return this.desktopConnectionService.getConnectedUsers(instance, isRoomLocked);
    }
}
