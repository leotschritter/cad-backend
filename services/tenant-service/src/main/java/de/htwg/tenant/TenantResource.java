package de.htwg.tenant;

import de.htwg.tenant.dto.CreateTenantRequest;
import de.htwg.tenant.dto.TenantCreatedResponse;
import de.htwg.tenant.dto.TenantResponse;
import de.htwg.tenant.model.Tenant;
import de.htwg.tenant.service.TenantService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

/**
 * REST API for tenant management.
 */
@Path("/api/v1/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Tenant Management", description = "Endpoints for managing tenants")
public class TenantResource {

    private static final Logger LOG = Logger.getLogger(TenantResource.class);

    @Inject
    TenantService tenantService;

    @Inject
    de.htwg.tenant.service.WorkflowMonitoringService workflowMonitoringService;

    /**
     * Create a new tenant (Standard tier).
     */
    @POST
    @Operation(summary = "Create a new tenant", 
               description = "Creates a new Standard tier tenant and triggers provisioning workflow")
    public Response createTenant(@Valid CreateTenantRequest request) {
        try {
            LOG.infof("📝 Received tenant creation request: %s", request.getName());
            
            Tenant tenant = tenantService.createTenant(request);
            
            TenantCreatedResponse response = TenantCreatedResponse.from(tenant);

            return Response.status(Response.Status.ACCEPTED)
                .entity(response)
                .build();

        } catch (IllegalArgumentException e) {
            LOG.warnf("⚠️ Invalid tenant creation request: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to create tenant: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to create tenant: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Get all tenants.
     */
    @GET
    @Operation(summary = "Get all tenants", 
               description = "Returns a list of all tenants with their current status")
    public Response getAllTenants() {
        try {
            List<TenantResponse> tenants = tenantService.getAllTenants();
            return Response.ok(tenants).build();

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to get tenants: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to retrieve tenants"))
                .build();
        }
    }

    /**
     * Get a specific tenant by tenant ID.
     */
    @GET
    @Path("/{tenantId}")
    @Operation(summary = "Get tenant by ID", 
               description = "Returns details of a specific tenant")
    public Response getTenant(@PathParam("tenantId") String tenantId) {
        try {
            Optional<Tenant> tenant = tenantService.getTenant(tenantId);

            if (tenant.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Tenant not found: " + tenantId))
                    .build();
            }

            return Response.ok(TenantResponse.from(tenant.get())).build();

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to get tenant %s: %s", tenantId, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to retrieve tenant"))
                .build();
        }
    }

    /**
     * Delete a tenant.
     */
    @DELETE
    @Path("/{tenantId}")
    @Operation(summary = "Delete a tenant", 
               description = "Triggers deprovisioning workflow to remove all tenant resources")
    public Response deleteTenant(@PathParam("tenantId") String tenantId) {
        try {
            LOG.infof("🗑️ Received tenant deletion request: %s", tenantId);
            
            tenantService.deleteTenant(tenantId);

            return Response.status(Response.Status.ACCEPTED)
                .entity(new SuccessResponse("Tenant deletion initiated. You will receive an email when complete."))
                .build();

        } catch (IllegalArgumentException e) {
            LOG.warnf("⚠️ Tenant not found: %s", tenantId);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(e.getMessage()))
                .build();

        } catch (IllegalStateException e) {
            LOG.warnf("⚠️ Invalid state for deletion: %s", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse(e.getMessage()))
                .build();

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to delete tenant %s: %s", tenantId, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to delete tenant: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Activate a tenant after backend deployment completes.
     * This endpoint should be called when the backend GitHub workflow completes successfully.
     * It will:
     * 1. Add the owner user to the Identity Platform tenant
     * 2. Trigger the frontend deployment
     * 3. Update tenant state to ACTIVE
     * 4. Send activation email
     */
    @POST
    @Path("/{tenantId}/activate")
    @Operation(summary = "Activate a tenant", 
               description = "Activates a tenant after backend provisioning completes. Triggers user creation and frontend deployment.")
    public Response activateTenant(@PathParam("tenantId") String tenantId) {
        try {
            LOG.infof("📥 Received activation request for tenant: %s", tenantId);
            
            workflowMonitoringService.activateTenant(tenantId);
            
            return Response.ok()
                .entity(new SuccessResponse("Tenant activated successfully. Frontend deployment initiated."))
                .build();

        } catch (IllegalArgumentException e) {
            LOG.warnf("⚠️ Tenant not found: %s", tenantId);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(e.getMessage()))
                .build();

        } catch (IllegalStateException e) {
            LOG.warnf("⚠️ Invalid state for activation: %s", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse(e.getMessage()))
                .build();

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to activate tenant %s: %s", tenantId, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to activate tenant: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Mark a tenant as failed if backend deployment fails.
     */
    @POST
    @Path("/{tenantId}/fail")
    @Operation(summary = "Mark tenant as failed", 
               description = "Marks a tenant as failed if backend provisioning fails")
    public Response failTenant(
            @PathParam("tenantId") String tenantId,
            @QueryParam("error") String errorMessage) {
        try {
            LOG.infof("📥 Received failure notification for tenant: %s", tenantId);
            
            workflowMonitoringService.markTenantAsFailed(
                tenantId, 
                errorMessage != null ? errorMessage : "Backend deployment failed"
            );
            
            return Response.ok()
                .entity(new SuccessResponse("Tenant marked as failed"))
                .build();

        } catch (IllegalArgumentException e) {
            LOG.warnf("⚠️ Tenant not found: %s", tenantId);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(e.getMessage()))
                .build();

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to mark tenant as failed %s: %s", tenantId, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to mark tenant as failed: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Manually complete tenant provisioning (trigger frontend deployment and user creation).
     * This endpoint can be used when backend deployment is complete and you want to
     * immediately activate the tenant without waiting for the automatic check.
     */
    @POST
    @Path("/{tenantId}/complete-provisioning")
    @Operation(summary = "Complete tenant provisioning", 
               description = "Manually triggers frontend deployment and user creation after backend deployment completes")
    public Response completeTenantProvisioning(@PathParam("tenantId") String tenantId) {
        try {
            LOG.infof("📥 Received manual provisioning completion request for tenant: %s", tenantId);
            
            Optional<Tenant> optTenant = tenantService.getTenant(tenantId);
            if (optTenant.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Tenant not found: " + tenantId))
                    .build();
            }
            
            Tenant tenant = optTenant.get();
            
            // Can only complete provisioning if in PROVISIONING state
            if (tenant.state != Tenant.ProvisioningState.PROVISIONING) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Tenant must be in PROVISIONING state. Current state: " + tenant.state))
                    .build();
            }
            
            // Call the activation method
            workflowMonitoringService.activateTenant(tenant.tenantId);
            
            return Response.ok()
                .entity(new SuccessResponse("Provisioning completed. Frontend deployment triggered and user created."))
                .build();

        } catch (IllegalArgumentException e) {
            LOG.warnf("⚠️ Invalid request: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to complete provisioning for tenant %s: %s", tenantId, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to complete provisioning: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Manually trigger Terraform destroy for a tenant in DEPROVISIONING state.
     * This endpoint can be used when Kubernetes cleanup is complete and you want to
     * immediately trigger Terraform destroy without waiting for the automatic check.
     */
    @POST
    @Path("/{tenantId}/destroy-infrastructure")
    @Operation(summary = "Trigger Terraform destroy", 
               description = "Manually triggers Terraform destroy for a tenant after Kubernetes cleanup completes")
    public Response destroyTenantInfrastructure(@PathParam("tenantId") String tenantId) {
        try {
            LOG.infof("📥 Received manual Terraform destroy request for tenant: %s", tenantId);
            
            Optional<Tenant> optTenant = tenantService.getTenant(tenantId);
            if (optTenant.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Tenant not found: " + tenantId))
                    .build();
            }
            
            Tenant tenant = optTenant.get();
            
            // Can only trigger Terraform destroy if in DEPROVISIONING state
            if (tenant.state != Tenant.ProvisioningState.DEPROVISIONING) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Tenant must be in DEPROVISIONING state to destroy infrastructure. Current state: " + tenant.state))
                    .build();
            }
            
            // Call the private method via reflection or make it public
            // For now, let's make the triggerTerraformDestroy method public in WorkflowMonitoringService
            workflowMonitoringService.triggerTerraformDestroyManually(tenant);
            
            return Response.ok()
                .entity(new SuccessResponse("Terraform destroy triggered. Infrastructure will be cleaned up shortly."))
                .build();

        } catch (IllegalArgumentException e) {
            LOG.warnf("⚠️ Invalid request: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to trigger Terraform destroy for tenant %s: %s", tenantId, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to trigger Terraform destroy: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Retrigger a failed tenant deployment.
     * Always restarts provisioning from the beginning (Terraform), regardless of where it failed.
     */
    @POST
    @Path("/{tenantId}/retrigger")
    @Operation(summary = "Retrigger failed tenant deployment", 
               description = "Retriggers a failed tenant deployment from the beginning (Terraform provisioning). All previous deployment state is cleared.")
    public Response retriggerFailedTenant(@PathParam("tenantId") String tenantId) {
        try {
            LOG.infof("🔄 Received retrigger request for tenant: %s", tenantId);
            
            tenantService.retriggerFailedTenant(tenantId);
            
            return Response.ok()
                .entity(new SuccessResponse("Tenant deployment retriggered successfully. Provisioning will restart from the beginning (Terraform)."))
                .build();

        } catch (IllegalArgumentException e) {
            LOG.warnf("⚠️ Invalid retrigger request: %s", e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(e.getMessage()))
                .build();

        } catch (IllegalStateException e) {
            LOG.warnf("⚠️ Invalid state for retrigger: %s", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse(e.getMessage()))
                .build();

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to retrigger tenant %s: %s", tenantId, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to retrigger tenant deployment: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Health check endpoint.
     */
    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Health check", 
               description = "Simple health check endpoint")
    public String health() {
        return "Tenant Service is running";
    }

    // Helper response classes
    public record ErrorResponse(String error) {}
    public record SuccessResponse(String message) {}
}

