package de.htwg.config;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * OpenAPI configuration for the Recommendation Service API.
 * Defines the Bearer token authentication scheme for Google Cloud Identity Platform.
 * This ensures that the Swagger UI will show an "Authorize" button for authentication.
 *
 * Server URL is configured via mp.openapi.servers property in application.properties.
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Recommendation Service API",
        version = "1.0.0",
        description = """
            API for personalized travel itinerary recommendations and social graph management.
            
            ## Authentication
            
            All endpoints require authentication using Google Cloud Identity Platform (Firebase Authentication).
            """,
        contact = @Contact(
            name = "Recommendation Service Team"
        )
    )
)
@SecurityScheme(
    securitySchemeName = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = """
        Firebase ID Token authentication (Google Cloud Identity Platform).
        
        **How to use:**
        1. Click the "Authorize" button (🔓) at the top of this page
        2. Enter: `Bearer <your-firebase-id-token>`
        3. Click "Authorize"
        
        **Example:**
        ```
        Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IjFlM...
        ```
        
        **Note:** The token must include the "Bearer " prefix.
        """,
    in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig extends Application {
    // This class is only used for OpenAPI annotations
    // No implementation needed - Quarkus handles the rest
}

