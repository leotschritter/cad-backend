package de.htwg.security;

import jakarta.enterprise.context.RequestScoped;
import lombok.Getter;
import lombok.Setter;

/**
 * Request-scoped bean that holds the authenticated user information for the current request.
 * This allows any service or endpoint to access the current user's information.
 */
@Setter
@Getter
@RequestScoped
public class SecurityContext {

    private AuthenticatedUser currentUser;

    /**
     * Checks if there is an authenticated user in the current request.
     *
     * @return true if user is authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return currentUser != null;
    }

    /**
     * Gets the user ID of the current authenticated user.
     *
     * @return The user ID, or null if not authenticated
     */
    public String getCurrentUserId() {
        return currentUser != null ? currentUser.getUid() : null;
    }

    /**
     * Gets the email of the current authenticated user.
     *
     * @return The email, or null if not authenticated
     */
    public String getCurrentUserEmail() {
        return currentUser != null ? currentUser.getEmail() : null;
    }
}

