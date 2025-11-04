package de.htwg.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Service for handling Google Cloud Identity Platform authentication token verification.
 * This service uses Firebase Admin SDK to verify ID tokens issued by Identity Platform.
 * Firebase Authentication and Identity Platform are the same backend service.
 */
@Getter
@ApplicationScoped
public class AuthenticationService {

    private static final Logger LOG = Logger.getLogger(AuthenticationService.class);

    @ConfigProperty(name = "identity-platform.auth.enabled", defaultValue = "true")
    boolean authEnabled;

    /**
     * Verifies an Identity Platform ID token and returns the authenticated user information.
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
            return Optional.of(decodedToken);
        } catch (FirebaseAuthException e) {
            LOG.warn("Failed to verify Identity Platform token: " + e.getMessage());
            return Optional.empty();
        }
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

