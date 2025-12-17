package de.htwg.tenant;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Simple greeting resource to verify the service is running.
 * This can be replaced with actual tenant service endpoints.
 */
@Path("/tenants")
public class TenantResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Tenant Service!";
    }
}

