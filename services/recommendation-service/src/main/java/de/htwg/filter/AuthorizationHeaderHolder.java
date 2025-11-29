package de.htwg.filter;

import jakarta.enterprise.context.RequestScoped;

/**
 * Request-scoped storage for the authorization header.
 * This allows us to forward the authentication token to other services.
 */
@RequestScoped
public class AuthorizationHeaderHolder {
    
    private String authorizationHeader;

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }
}

