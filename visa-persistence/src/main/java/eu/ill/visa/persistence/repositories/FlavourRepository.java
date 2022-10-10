package eu.ill.visa.persistence.repositories;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import eu.ill.visa.core.domain.*;
import eu.ill.visa.persistence.providers.FlavourFilterProvider;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;

@Singleton
public class FlavourRepository extends AbstractRepository<Flavour> {

    @Inject
    FlavourRepository(final Provider<EntityManager> entityManager) {
        super(entityManager);
    }

    @Override
    public List<Flavour> getAll() {
        final TypedQuery<Flavour> query = getEntityManager().createNamedQuery("flavour.getAll", Flavour.class);
        return query.getResultList();
    }

    public List<Flavour> getAllForAdmin() {
        final TypedQuery<Flavour> query = getEntityManager().createNamedQuery("flavour.getAllForAdmin", Flavour.class);
        return query.getResultList();
    }

    public List<Flavour> getAll(Pagination pagination) {
        final TypedQuery<Flavour> query = getEntityManager().createNamedQuery("flavour.getAll", Flavour.class);
        if (pagination != null) {
            final int offset = pagination.getOffset();
            final int limit = pagination.getLimit();
            query.setFirstResult(offset);
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }

    public List<Flavour> getAll(QueryFilter filter, OrderBy orderBy, Pagination pagination) {
        final FlavourFilterProvider provider = new FlavourFilterProvider(getEntityManager());
        return super.getAll(provider, filter, orderBy, pagination);
    }

    public Long countAll(QueryFilter filter) {
        final FlavourFilterProvider provider = new FlavourFilterProvider(getEntityManager());
        return super.countAll(provider, filter);
    }

    public Long countAllForAdmin() {
        TypedQuery<Long> query = getEntityManager().createNamedQuery("flavour.countAllForAdmin", Long.class);
        return query.getSingleResult();
    }

    public Flavour getById(Long id) {
        try {
            TypedQuery<Flavour> query = getEntityManager().createNamedQuery("flavour.getById", Flavour.class);
            query.setParameter("id", id);
            return query.getSingleResult();
        } catch (
            NoResultException exception) {
            return null;
        }
    }

    public void delete(Flavour flavour) {
        remove(flavour);
    }

    public void save(final Flavour flavour) {
        if (flavour.getId() == null) {
            persist(flavour);

        } else {
            merge(flavour);
        }
    }

    public void create(Flavour flavour) {
        persist(flavour);
    }


}
