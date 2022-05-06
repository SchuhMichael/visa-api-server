package eu.ill.visa.business.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import eu.ill.visa.cloud.exceptions.CloudException;
import eu.ill.visa.cloud.http.HttpClient;
import eu.ill.visa.core.domain.*;
import eu.ill.visa.persistence.repositories.FlavourRepository;
import static eu.ill.visa.cloud.http.HttpMethod.*;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNullElseGet;

@Transactional
@Singleton
public class FlavourService {

    private FlavourRepository repository;
    private HttpClient client;

    @Inject
    public FlavourService(FlavourRepository repository) {
        this.repository = repository;
    }

    public List<Flavour> getAll() {
        return this.repository.getAll();
    }

    public Flavour getById(Long id) {
        return this.repository.getById(id);
    }

    public void delete(Flavour flavour) {
        this.repository.delete(flavour);
    }

    public void save(@NotNull Flavour flavour) {
        this.repository.save(flavour);
    }

    public void create(Flavour flavour) {
        this.repository.create(flavour);
    }

    public List<Flavour> getAll(OrderBy orderBy, Pagination pagination) {
        return this.getAll(new QueryFilter(), orderBy, pagination);
    }

    public List<Flavour> getAll(QueryFilter filter, OrderBy orderBy, Pagination pagination) {
        return this.repository.getAll(filter, orderBy, pagination);
    }

    public List<Flavour> getAll( Pagination pagination) {
        return this.repository.getAll( pagination);
    }

    public Long countAll() {
        return repository.countAll(new QueryFilter());
    }

    public Long countAllForAdmin() {
        return repository.countAllForAdmin();
    }

    public Long countAll(QueryFilter filter) {
        return repository.countAll(requireNonNullElseGet(filter, QueryFilter::new));
    }


}
