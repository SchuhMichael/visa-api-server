package eu.ill.visa.vdi.models;

import com.corundumstudio.socketio.SocketIOClient;
import eu.ill.visa.core.domain.enumerations.InstanceActivityType;
import eu.ill.visa.vdi.concurrency.ConnectionThread;

import java.util.Date;

public class DesktopConnection {

    private final String connectionId;
    private final SocketIOClient client;
    private final Long instanceId;
    private Date lastSeenAt;
    private Date lastInteractionAt = new Date();
    private InstanceActivityType instanceActivityType;
    private final ConnectedUser connectedUser;
    private ConnectionThread connectionThread;
    private final String roomId;
    private boolean isRoomLocked = false;

    public DesktopConnection(String connectionId, SocketIOClient client, Long instanceId, Date lastSeenAt, final ConnectedUser connectedUser, String roomId) {
        this.connectionId = connectionId;
        this.client = client;
        this.instanceId = instanceId;
        this.lastSeenAt = lastSeenAt;
        this.connectedUser = connectedUser;
        this.roomId = roomId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public SocketIOClient getClient() {
        return client;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public Date getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Date lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Date getLastInteractionAt() {
        return lastInteractionAt;
    }

    public void setLastInteractionAt(Date lastInteractionAt) {
        this.lastInteractionAt = lastInteractionAt;
    }

    public InstanceActivityType getInstanceActivity() {
        return instanceActivityType;
    }

    public void resetInstanceActivity() {
        this.instanceActivityType = null;
    }

    public void setInstanceActivity(InstanceActivityType instanceActivityType) {
        if (this.instanceActivityType == null) {
            this.instanceActivityType = instanceActivityType;

        } else if (instanceActivityType.equals(InstanceActivityType.MOUSE) && this.instanceActivityType.equals(InstanceActivityType.KEYBOARD)) {
            this.instanceActivityType = InstanceActivityType.MOUSE_AND_KEYBOARD;

        } else if (instanceActivityType.equals(InstanceActivityType.KEYBOARD) && this.instanceActivityType.equals(InstanceActivityType.MOUSE)) {
            this.instanceActivityType = InstanceActivityType.MOUSE_AND_KEYBOARD;
        }
        this.updateLastInteractionAt();
    }

    public void updateLastInteractionAt() {
        this.lastInteractionAt = new Date();
    }

    public ConnectedUser getConnectedUser() {
        return connectedUser;
    }

    public ConnectionThread getConnectionThread() {
        return connectionThread;
    }

    public void setConnectionThread(ConnectionThread connectionThread) {
        this.connectionThread = connectionThread;
    }

    public String getRoomId() {
        return this.roomId;
    }

    public boolean isRoomLocked() {
        return isRoomLocked;
    }

    public void setRoomLocked(boolean roomLocked) {
        isRoomLocked = roomLocked;
    }
}
