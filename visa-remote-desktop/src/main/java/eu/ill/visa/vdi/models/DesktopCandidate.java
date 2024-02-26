package eu.ill.visa.vdi.models;

import com.corundumstudio.socketio.SocketIOClient;
import eu.ill.visa.core.domain.User;

public class DesktopCandidate {

    private final String connectionId;
    private final SocketIOClient client;
    private final User user;
    private final Long instanceId;

    public DesktopCandidate(String connectionId, SocketIOClient client, User user, Long instanceId) {
        this.connectionId = connectionId;
        this.client = client;
        this.user = user;
        this.instanceId = instanceId;
    }

    public String getConnectionId() {
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
