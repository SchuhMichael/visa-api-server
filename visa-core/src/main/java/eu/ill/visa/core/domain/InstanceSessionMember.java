package eu.ill.visa.core.domain;

import java.util.Date;
import java.util.UUID;

public class InstanceSessionMember extends Timestampable {

    private Long id;
    private InstanceSession instanceSession;
    private UUID connectionId;
    private User user;
    private String role;
    private boolean active = false;
    private Date lastSeenAt = new Date();
    private Date lastInteractionAt = new Date();


    public InstanceSessionMember() {
    }

    public InstanceSessionMember(InstanceSession instanceSession, UUID connectionId, User user, String role) {
        this.instanceSession = instanceSession;
        this.connectionId = connectionId;
        this.user = user;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InstanceSession getInstanceSession() {
        return instanceSession;
    }

    public void setInstanceSession(InstanceSession instanceSession) {
        this.instanceSession = instanceSession;
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(UUID connectionId) {
        this.connectionId = connectionId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Date getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Date lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public void updateLastSeenAt() {
        this.lastSeenAt = new Date();
    }

    public Date getLastInteractionAt() {
        return lastInteractionAt;
    }

    public void setLastInteractionAt(Date lastInteractionAt) {
        this.lastInteractionAt = lastInteractionAt;
    }
}
