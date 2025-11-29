package de.htwg.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an authenticated user with information extracted from Google Cloud Identity Platform token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser {
    /**
     * The unique Firebase user ID (UID)
     */
    private String uid;

    /**
     * The user's email address
     */
    private String email;

    /**
     * The user's display name
     */
    private String name;

    /**
     * Whether the user's email is verified
     */
    private boolean emailVerified;
}

