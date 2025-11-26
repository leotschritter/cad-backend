package de.htwg.security;

import com.google.firebase.auth.FirebaseToken;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * JAX-RS filter that intercepts requests to endpoints annotated with @Authenticated
 * and validates the Google Cloud Identity Platform authentication token.
 * The filter checks for tokens in a configurable primary header (e.g., X-Forwarded-Authorization for API Gateway)
 * with fallback to a secondary header (e.g., Authorization for direct access/Swagger UI).
 * This allows the backend to work both behind API Gateway and for direct access.
 * Tokens are verified using Firebase Admin SDK (the official SDK for Identity Platform).
 */
@Provider
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(AuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Inject
    AuthenticationService authenticationService;

    @Inject
    SecurityContext securityContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Always allow OPTIONS requests (CORS preflight) without authentication
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            LOG.debug("OPTIONS request (CORS preflight), allowing without authentication");
            return;
        }

        // If authentication is disabled, allow all requests
        if (!authenticationService.isAuthEnabled()) {
            LOG.debug("Authentication is disabled, allowing request");
            return;
        }

        // Extract token from configured headers (primary with fallback)
        String primaryHeaderName = authenticationService.getPrimaryAuthHeader();
        String fallbackHeaderName = authenticationService.getFallbackAuthHeader();

        String authHeader = requestContext.getHeaderString(primaryHeaderName);

        // Fallback to secondary header if primary is not present
        if (authHeader == null || authHeader.isEmpty()) {
            authHeader = requestContext.getHeaderString(fallbackHeaderName);
            LOG.debug("Primary header '" + primaryHeaderName + "' not found, using fallback header '" + fallbackHeaderName + "'");
        } else {
            LOG.debug("Using primary header '" + primaryHeaderName + "' for authentication");
        }

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            LOG.warn("Missing or invalid authentication header (checked: " + primaryHeaderName + ", " + fallbackHeaderName + ")");
            abortWithUnauthorized(requestContext, "Missing or invalid authentication header");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // Verify the token
        Optional<FirebaseToken> firebaseToken = authenticationService.verifyToken(token);

        if (firebaseToken.isEmpty()) {
            LOG.warn("Invalid or expired token");
            abortWithUnauthorized(requestContext, "Invalid or expired token");
            return;
        }

        // Create authenticated user and set in security context
        FirebaseToken decodedToken = firebaseToken.get();
        AuthenticatedUser user = AuthenticatedUser.builder()
                .uid(decodedToken.getUid())
                .email(decodedToken.getEmail())
                .name((String) decodedToken.getClaims().get("name"))
                .emailVerified(decodedToken.isEmailVerified())
                .build();

        securityContext.setCurrentUser(user);
        LOG.debug("Request authenticated for user: " + user.getEmail());
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"" + message + "\"}")
                        .build()
        );
    }
}

