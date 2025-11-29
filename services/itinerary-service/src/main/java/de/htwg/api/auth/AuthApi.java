package de.htwg.api.auth;

import de.htwg.security.Authenticated;
import de.htwg.security.AuthenticatedUser;
import de.htwg.security.SecurityContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * API endpoints for authentication-related operations.
 * Demonstrates how to use the @Authenticated annotation and SecurityContext.
 */
@Path("/auth")
@Tag(name = "Authentication", description = "Authentication-related endpoints")
public class AuthApi {

    @Inject
    SecurityContext securityContext;

    @GET
    @Path("/me")
    @Authenticated  // This endpoint requires authentication
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get current user information",
        description = "Returns the authenticated user's information from the Identity Platform token. Requires valid Identity Platform ID token in Authorization header."
    )
    @SecurityRequirement(name = "BearerAuth")
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "User information retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(implementation = AuthenticatedUser.class)
            )
        ),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized - Missing or invalid token",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON
            )
        )
    })
    public Response getCurrentUser() {
        AuthenticatedUser user = securityContext.getCurrentUser();
        
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"User not authenticated\"}")
                    .build();
        }
        
        return Response.ok(user).build();
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Health check endpoint",
        description = "Public endpoint to check if authentication system is working. Does not require authentication."
    )
    @APIResponses(value = {
        @APIResponse(
            responseCode = "200",
            description = "Authentication system is healthy"
        )
    })
    public Response healthCheck() {
        return Response.ok()
                .entity("{\"status\": \"ok\", \"message\": \"Authentication system is operational\"}")
                .build();
    }
}

