package eu.ill.visa.vdi.models;

import com.corundumstudio.socketio.SocketIOClient;
import eu.ill.visa.core.domain.User;

import java.util.UUID;

public class DesktopCandidate {

    private final UUID connectionId;
    private final SocketIOClient client;
    private final User user;
    private final Long instanceId;

    public DesktopCandidate(UUID connectionId, SocketIOClient client, User user, Long instanceId) {
        this.connectionId = connectionId;
        this.client = client;
        this.user = user;
        this.instanceId = instanceId;
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public SocketIOClient getClient() {
        return client;
    }

    public User getUser() {
        return user;
    }

    public String getRoomId() {
        return this.instanceId.toString();
    }

    public Long getInstanceId() {
        return instanceId;
    }
}
