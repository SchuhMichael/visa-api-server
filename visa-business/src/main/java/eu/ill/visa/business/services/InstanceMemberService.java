package eu.ill.visa.business.services;

import eu.ill.visa.core.entity.Instance;
import eu.ill.visa.core.entity.InstanceMember;
import eu.ill.visa.core.entity.User;
import eu.ill.visa.core.entity.enumerations.InstanceMemberRole;
import eu.ill.visa.persistence.repositories.InstanceMemberRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static eu.ill.visa.core.entity.enumerations.InstanceMemberRole.OWNER;

@Transactional
@Singleton
public class InstanceMemberService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceMemberService.class);

    private final InstanceMemberRepository repository;

    @Inject
    public InstanceMemberService(final InstanceMemberRepository repository) {
        this.repository = repository;
    }

    public List<InstanceMember> getAll() {
        return this.repository.getAll();
    }

    public InstanceMember getById(@NotNull Long id) {
        return this.repository.getById(id);
    }

    public InstanceMember getByInstanceAndUser(Instance instance, User user) {
        return this.repository.getByInstanceAndUser(instance, user);
    }

    public List<InstanceMember> getAllByInstanceId(Long instanceId) {
        return this.repository.getAllByInstanceId(instanceId);
    }

    public List<InstanceMember> getAllByInstanceIdAndRole(Long instanceId, InstanceMemberRole role) {
        return this.repository.getAllByInstanceIdAndRole(instanceId, role);
    }

    public User getOwnerByInstanceId(Long instanceId) {
        return repository.getAllByInstanceIdAndRole(instanceId, OWNER).stream()
            .findFirst()
            .map(InstanceMember::getUser)
            .orElse(null);
    }

    public void save(@NotNull InstanceMember instanceMember) {
        this.repository.save(instanceMember);
    }
}
