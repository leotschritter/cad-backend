package de.htwg.config;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

/**
 * OpenAPI configuration for the Travel App API.
 * Defines the Bearer token authentication scheme for Google Cloud Identity Platform.
 * This ensures that generated API clients (from openapi-generator) will properly
 * handle authentication tokens.
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Travel App API",
        version = "1.0.0",
        description = """
            API for managing travel itineraries, locations, and social interactions.
            
            ## Authentication
            
            Most endpoints require authentication using Google Cloud Identity Platform (Firebase Authentication).
            
            ### How to Authenticate:
            
            1. **Get a Firebase ID token** from your frontend authentication flow
            2. **Include the token** in the `Authorization` header of your requests:
               ```
               Authorization: Bearer <your-firebase-id-token>
               ```
            
            
            
            """,
        contact = @Contact(
            name = "Travel App Team"
        )
    ),
    servers = {
        @Server(url = "https://itinerary.tripico.fun", description = "Production server"),
        @Server(url = "http://localhost:8080", description = "Local development server")
    }
)
@SecurityScheme(
    securitySchemeName = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = """
        Firebase ID Token authentication (Google Cloud Identity Platform).
        
        **Production:** Include your Firebase ID token in the Authorization header.
        
        **Local Development:** Include your Firebase ID token in the Authorization header.
        
        **Example:**
        ```
        Authorization: Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IjFlM...
        ```
        """,
    in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig extends Application {
    // This class is only used for OpenAPI annotations
    // No implementation needed - Quarkus handles the rest
}



