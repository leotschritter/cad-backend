package de.htwg.tenant.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.panache.common.Sort;
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
     * Sanitized tenant ID used for identification (e.g., "acme-corp")
     */
    public String tenantId;

    /**
     * Numeric tenant identifier for namespace (e.g., 1, 2, 3)
     * Used to create namespace: standard-{tenantNumber}
     * Only used for STANDARD tier
     */
    public Integer tenantNumber;

    /**
     * Enterprise name for enterprise tier (e.g., "acme-corp", "company-x")
     * Used to create namespace: enterprise-{enterpriseName}
     * Only used for ENTERPRISE tier
     */
    public String enterpriseName;

    /**
     * Frontend subdomain (e.g., "frontend-standard-1.tripico.fun")
     */
    public String frontendDomain;

    /**
     * API Gateway URL for this tenant (e.g., "https://api-standard-1.tripico.fun")
     */
    public String apiGatewayUrl;

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
     * Standard: "standard-1", "standard-2", etc.
     * Enterprise: "enterprise-acme-corp", "enterprise-company-x", etc.
     */
    public String namespace;

    /**
     * Kubernetes cluster name
     * Standard: "tripico-cluster" (shared cluster)
     * Enterprise: "tripico-{enterpriseName}-cluster" (dedicated cluster)
     */
    public String clusterName;

    /**
     * Identity Platform tenant ID (created via SDK)
     */
    public String identityPlatformTenantId;

    /**
     * Firestore database ID for this tenant
     */
    public String firestoreDatabaseId;

    /**
     * Owner's user UID in Identity Platform (created after provisioning)
     */
    public String ownerUid;

    /**
     * GitHub repository dispatch event ID (for tracking)
     */
    public String provisioningDispatchId;

    /**
     * GitHub repository dispatch event ID for deprovisioning
     */
    public String deprovisioningDispatchId;

    /**
     * GitHub repository dispatch event ID for frontend deployment
     */
    public String frontendDeploymentDispatchId;

    /**
     * GitHub workflow dispatch event ID for Terraform provisioning
     */
    public String terraformDispatchId;

    /**
     * GitHub workflow dispatch event ID for Terraform destroy
     */
    public String terraformDestroyDispatchId;

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

    public static Tenant findByTenantNumber(Integer tenantNumber) {
        return find("tenantNumber", tenantNumber).firstResult();
    }

    public static Tenant findByFrontendDomain(String frontendDomain) {
        return find("frontendDomain", frontendDomain).firstResult();
    }

    public static Tenant findByEnterpriseName(String enterpriseName) {
        return find("enterpriseName", enterpriseName).firstResult();
    }

    public static long countByState(ProvisioningState state) {
        return count("state", state);
    }

    public static Integer findMaxTenantNumber() {
        Tenant tenant = findAll(Sort.descending("tenantNumber"))
            .page(0, 1)
            .firstResult();
        return tenant != null ? tenant.tenantNumber : 0;
    }

    public enum ProvisioningState {
        PENDING,                  // Initial state, not yet started
        TERRAFORM_PROVISIONING,   // Terraform apply workflow is running
        PROVISIONING,             // Kubernetes/Helm deployment workflow is running
        ACTIVE,                   // Successfully provisioned and running
        DEPROVISIONING,           // Being deleted - Kubernetes cleanup in progress
        TERRAFORM_DESTROYING,     // Terraform destroy workflow is running
        DELETED,                  // Successfully deleted
        FAILED                    // Provisioning or deprovisioning failed
    }

    public enum TenantTier {
        FREE,           // Shared pool (future)
        STANDARD,       // Namespace isolation
        ENTERPRISE      // Dedicated cluster (future)
    }
}

