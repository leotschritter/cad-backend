package de.htwg.tenant.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Sample Tenant entity for MongoDB.
 * This demonstrates how to use MongoDB with Panache.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@MongoEntity(collection = "tenants")
public class Tenant extends PanacheMongoEntity {

    public String name;
    public String domain;
    public String description;
    public boolean active = true;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime updatedAt = LocalDateTime.now();

    // You can add custom methods here
    public static Tenant findByDomain(String domain) {
        return find("domain", domain).firstResult();
    }

    public static long countActive() {
        return count("active", true);
    }
}

