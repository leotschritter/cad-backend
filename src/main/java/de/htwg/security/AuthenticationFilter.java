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
 *
 * The filter expects the token in the Authorization header with "Bearer " prefix.
 * Tokens are verified using Firebase Admin SDK (the official SDK for Identity Platform).
 */
@Provider
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(AuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
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

        // Extract token from Authorization header
        String authHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            LOG.warn("Missing or invalid Authorization header");
            abortWithUnauthorized(requestContext, "Missing or invalid Authorization header");
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

