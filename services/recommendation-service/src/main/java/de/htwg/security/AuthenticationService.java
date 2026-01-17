package de.htwg.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Optional;

/**
 * Service for handling Google Cloud Identity Platform authentication token verification.
 * This service uses Firebase Admin SDK to verify ID tokens issued by Identity Platform.
 * Firebase Authentication and Identity Platform are the same backend service.
 *
 * Multi-tenancy support:
 * - Freemium tier: No tenant ID required (accepts any valid token)
 * - Standard/Enterprise tiers: Requires token to have matching tenant ID
 */
@Getter
@ApplicationScoped
public class AuthenticationService {

    private static final Logger LOG = Logger.getLogger(AuthenticationService.class);

    @ConfigProperty(name = "identity-platform.auth.enabled", defaultValue = "true")
    boolean authEnabled;

    @ConfigProperty(name = "identity-platform.auth.primary-header", defaultValue = "Authorization")
    String primaryAuthHeader;

    @ConfigProperty(name = "identity-platform.auth.fallback-header", defaultValue = "Authorization")
    String fallbackAuthHeader;

    /**
     * Expected Identity Platform tenant ID for this deployment.
     * - Empty string: Freemium tier - accepts any valid token (no tenant restriction)
     * - Non-empty: Standard/Enterprise tier - only accepts tokens from this specific tenant
     */
    @ConfigProperty(name = "identity-platform.tenant.expected-id", defaultValue = "")
    String expectedTenantId;

    /**
     * Verifies an Identity Platform ID token and returns the authenticated user information.
     * For non-freemium tiers, also validates that the token belongs to the expected tenant.
     *
     * @param idToken The Identity Platform ID token to verify
     * @return Optional containing FirebaseToken if valid, empty if invalid or auth is disabled
     */
    public Optional<FirebaseToken> verifyToken(String idToken) {
        if (!authEnabled) {
            LOG.debug("Identity Platform authentication is disabled, skipping token verification");
            return Optional.empty();
        }

        if (idToken == null || idToken.trim().isEmpty()) {
            LOG.debug("No token provided");
            return Optional.empty();
        }

        try {
            // Verify token using Firebase Admin SDK (official SDK for Identity Platform)
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            LOG.debug("Identity Platform token verified successfully for user: " + decodedToken.getUid());

            // Validate tenant if this is a non-freemium deployment
            if (!validateTenant(decodedToken)) {
                return Optional.empty();
            }

            return Optional.of(decodedToken);
        } catch (FirebaseAuthException e) {
            LOG.warn("Failed to verify Identity Platform token: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Validates that the token belongs to the expected tenant.
     * - If expectedTenantId is empty (freemium): accepts any token
     * - If expectedTenantId is set (standard/enterprise): requires exact tenant match
     *
     * @param token The decoded Firebase token
     * @return true if tenant validation passes, false otherwise
     */
    @SuppressWarnings("unchecked")
    private boolean validateTenant(FirebaseToken token) {
        // Freemium tier: no tenant restriction
        if (expectedTenantId == null || expectedTenantId.trim().isEmpty()) {
            LOG.debug("Freemium tier: no tenant validation required");
            return true;
        }

        // Extract tenant ID from token claims
        // Firebase stores tenant ID in claims under "firebase.tenant"
        String tokenTenantId = extractTenantId(token);

        if (tokenTenantId == null || tokenTenantId.trim().isEmpty()) {
            LOG.warn("Token has no tenant ID but this deployment requires tenant: " + expectedTenantId);
            return false;
        }

        if (!expectedTenantId.equals(tokenTenantId)) {
            LOG.warn("Token tenant ID '" + tokenTenantId + "' does not match expected tenant '" + expectedTenantId + "'");
            return false;
        }

        LOG.debug("Tenant validation passed for tenant: " + tokenTenantId);
        return true;
    }

    /**
     * Extracts the tenant ID from a Firebase token.
     * The tenant ID is stored in the "firebase" claim object under "tenant" key.
     *
     * @param token The decoded Firebase token
     * @return The tenant ID, or null if not present
     */
    @SuppressWarnings("unchecked")
    private String extractTenantId(FirebaseToken token) {
        Map<String, Object> claims = token.getClaims();
        if (claims == null) {
            return null;
        }

        Object firebaseClaim = claims.get("firebase");
        if (firebaseClaim instanceof Map) {
            Map<String, Object> firebaseMap = (Map<String, Object>) firebaseClaim;
            Object tenant = firebaseMap.get("tenant");
            if (tenant instanceof String) {
                return (String) tenant;
            }
        }

        return null;
    }

    /**
     * Extracts the user ID from an Identity Platform token.
     *
     * @param token The Identity Platform token
     * @return The user ID (UID)
     */
    public String getUserId(FirebaseToken token) {
        return token.getUid();
    }

    /**
     * Extracts the user email from an Identity Platform token.
     *
     * @param token The Identity Platform token
     * @return The user email, or null if not available
     */
    public String getUserEmail(FirebaseToken token) {
        return token.getEmail();
    }

    /**
     * Extracts the user name from an Identity Platform token.
     *
     * @param token The Identity Platform token
     * @return The user name, or null if not available
     */
    public String getUserName(FirebaseToken token) {
        return (String) token.getClaims().get("name");
    }

}

