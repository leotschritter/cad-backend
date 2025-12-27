package de.htwg.tenant.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Tenant entity representing a Standard tier tenant in the system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@MongoEntity(collection = "tenants")
public class Tenant extends PanacheMongoEntity {

    /**
     * Human-readable tenant name (e.g., "Acme Corp")
     */
    public String name;

    /**
     * Sanitized tenant ID used for namespaces, subdomains, etc. (e.g., "acme-corp")
     */
    public String tenantId;

    /**
     * Full subdomain (e.g., "acme-corp.tripico.fun")
     */
    public String subdomain;

    /**
     * Email of the user who created this tenant
     */
    public String ownerEmail;

    /**
     * Temporary storage for user password (only during provisioning)
     * This will be cleared after Identity Platform user is created
     */
    public String ownerPasswordHash;

    /**
     * Current provisioning state
     */
    public ProvisioningState state = ProvisioningState.PENDING;

    /**
     * Tier type (currently only STANDARD is supported)
     */
    public TenantTier tier = TenantTier.STANDARD;

    /**
     * Kubernetes namespace name
     */
    public String namespace;

    /**
     * GitHub Actions workflow run ID for provisioning
     */
    public Long provisioningWorkflowRunId;

    /**
     * GitHub Actions workflow run ID for deprovisioning
     */
    public Long deprovisioningWorkflowRunId;

    /**
     * Additional error message if provisioning/deprovisioning failed
     */
    public String errorMessage;

    /**
     * When the tenant was created
     */
    public LocalDateTime createdAt = LocalDateTime.now();

    /**
     * When the tenant was last updated
     */
    public LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * When provisioning completed successfully
     */
    public LocalDateTime activatedAt;

    /**
     * When deprovisioning was requested
     */
    public LocalDateTime deletedAt;

    // Static query methods
    public static Tenant findByTenantId(String tenantId) {
        return find("tenantId", tenantId).firstResult();
    }

    public static Tenant findBySubdomain(String subdomain) {
        return find("subdomain", subdomain).firstResult();
    }

    public static long countByState(ProvisioningState state) {
        return count("state", state);
    }

    public enum ProvisioningState {
        PENDING,        // Initial state, not yet started
        PROVISIONING,   // GitHub workflow is running
        ACTIVE,         // Successfully provisioned and running
        DEPROVISIONING, // Being deleted
        DELETED,        // Successfully deleted
        FAILED          // Provisioning or deprovisioning failed
    }

    public enum TenantTier {
        FREE,           // Shared pool (future)
        STANDARD,       // Namespace isolation
        ENTERPRISE      // Dedicated cluster (future)
    }
}

