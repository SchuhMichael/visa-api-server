package eu.ill.visa.core.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class InstanceSession extends Timestampable {

    private Long id;
    private String remoteDesktopConnectionId;
    private Instance instance;
    private Boolean current;

    public InstanceSession() {
    }

    public InstanceSession(Instance instance, String remoteDesktopConnectionId) {
        this.instance = instance;
        this.remoteDesktopConnectionId = remoteDesktopConnectionId;
        this.current = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public String getRemoteDesktopConnectionId() {
        return remoteDesktopConnectionId;
    }

    public void setRemoteDesktopConnectionId(String remoteDesktopConnectionId) {
        this.remoteDesktopConnectionId = remoteDesktopConnectionId;
    }

    public Boolean getCurrent() {
        return current;
    }

    public void setCurrent(Boolean current) {
        this.current = current;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        InstanceSession that = (InstanceSession) o;

        return new EqualsBuilder()
            .append(id, that.id)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(id)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("connectionId", remoteDesktopConnectionId)
            .append("current", current)
            .toString();
    }
}
