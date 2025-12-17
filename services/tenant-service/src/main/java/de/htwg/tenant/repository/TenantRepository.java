package de.htwg.tenant.repository;

import de.htwg.tenant.model.Tenant;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Repository for Tenant entity.
 * This demonstrates how to create a repository with custom queries.
 */
@ApplicationScoped
public class TenantRepository implements PanacheMongoRepository<Tenant> {

    public List<Tenant> findByActive(boolean active) {
        return list("active", active);
    }

    public Tenant findByName(String name) {
        return find("name", name).firstResult();
    }

    public List<Tenant> findByNameContaining(String searchTerm) {
        return list("name like ?1", "%" + searchTerm + "%");
    }
}

