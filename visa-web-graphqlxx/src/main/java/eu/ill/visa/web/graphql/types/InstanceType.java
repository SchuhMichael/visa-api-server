package eu.ill.visa.web.graphql.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.ill.visa.core.entity.Instance;
import eu.ill.visa.core.entity.enumerations.InstanceState;
import io.smallrye.graphql.api.AdaptToScalar;
import io.smallrye.graphql.api.Scalar;
import org.eclipse.microprofile.graphql.Type;

import java.util.Date;
import java.util.List;

@Type("Instance")
public class InstanceType {

    @AdaptToScalar(Scalar.Int.class)
    private final Long id;
    private final String uid;
    private final String name;
    private final String comments;
    private final InstanceState state;
    private final List<InstanceMemberType> members;
    private final PlanType plan;
    private final List<ExperimentType> experiments;
    private final Date createdAt;
    private final Date lastSeenAt;
    private final Date lastInteractionAt;
    private final Date terminationDate;
    private final String username;
    private final String keyboardLayout;
    private final List<InstanceAttributeType> attributes;
    private final Long cloudId;
    private final String computeId;

    public InstanceType(final Instance instance) {
        this.id = instance.getId();
        this.uid = instance.getUid();
        this.name = instance.getName();
        this.comments = instance.getComments();
        this.state = instance.getState();
        this.members = instance.getMembers().stream().map(InstanceMemberType::new).toList();
        this.plan = new PlanType(instance.getPlan());
        this.experiments = instance.getExperiments().stream().map(ExperimentType::new).toList();
        this.createdAt = instance.getCreatedAt();
        this.lastSeenAt = instance.getLastSeenAt();
        this.lastInteractionAt = instance.getLastInteractionAt();
        this.terminationDate = instance.getTerminationDate();
        this.username = instance.getUsername();
        this.keyboardLayout = instance.getKeyboardLayout();
        this.attributes = instance.getAttributes().stream().map(InstanceAttributeType::new).toList();
        this.cloudId = instance.getCloudId();
        this.computeId = instance.getComputeId();
    }

    public Long getId() {
        return id;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getComments() {
        return comments;
    }

    public InstanceState getState() {
        return state;
    }

    public List<InstanceMemberType> getMembers() {
        return members;
    }

    public PlanType getPlan() {
        return plan;
    }

    public List<ExperimentType> getExperiments() {
        return experiments;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getLastSeenAt() {
        return lastSeenAt;
    }

    public Date getLastInteractionAt() {
        return lastInteractionAt;
    }

    public Date getTerminationDate() {
        return terminationDate;
    }

    public String getUsername() {
        return username;
    }

    public String getKeyboardLayout() {
        return keyboardLayout;
    }

    public List<InstanceAttributeType> getAttributes() {
        return attributes;
    }

    @JsonIgnore
    public Long getCloudId() {
        return cloudId;
    }

    @JsonIgnore
    public String getComputeId() {
        return computeId;
    }
}
