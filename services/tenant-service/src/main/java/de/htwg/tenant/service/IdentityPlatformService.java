package de.htwg.tenant.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing Identity Platform users and tenants.
 */
@ApplicationScoped
public class IdentityPlatformService {

    private static final Logger LOG = Logger.getLogger(IdentityPlatformService.class);
    private static final String IDENTITY_PLATFORM_API_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signUp";

    @ConfigProperty(name = "identity-platform.api-key")
    String apiKey;

    /**
     * Add a user to an Identity Platform tenant.
     * 
     * @param tenantId The Identity Platform tenant ID
     * @param email User's email address
     * @param password User's password
     * @return The user's UID in Identity Platform
     */
    public String addUserToTenant(String tenantId, String email, String password) {
        Client client = ClientBuilder.newClient();
        
        try {
            // Build the request URL with API key
            String requestUrl = IDENTITY_PLATFORM_API_URL + "?key=" + apiKey;
            
            // Build the request payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantId", tenantId);
            payload.put("email", email);
            payload.put("password", password);
            payload.put("returnSecureToken", true);
            
            LOG.infof("👤 Creating user in Identity Platform tenant %s: %s", tenantId, email);
            
            // Send the request
            Response response = client.target(requestUrl)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(payload));
            
            if (response.getStatus() == 200) {
                Map<String, Object> responseData = response.readEntity(Map.class);
                String uid = (String) responseData.get("localId");
                
                LOG.infof("✅ User created successfully in Identity Platform: %s (UID: %s)", email, uid);
                return uid;
                
            } else {
                String errorBody = response.readEntity(String.class);
                LOG.errorf("❌ Failed to create user in Identity Platform. Status: %d, Body: %s", 
                    response.getStatus(), errorBody);
                throw new RuntimeException("Failed to create user in Identity Platform: " + errorBody);
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "❌ Error creating user in Identity Platform: %s", e.getMessage());
            throw new RuntimeException("Failed to add user to Identity Platform tenant", e);
            
        } finally {
            client.close();
        }
    }

    /**
     * Delete a user from Identity Platform.
     * 
     * @param idToken The ID token of the user to delete
     */
    public void deleteUser(String idToken) {
        Client client = ClientBuilder.newClient();
        
        try {
            String requestUrl = "https://identitytoolkit.googleapis.com/v1/accounts:delete?key=" + apiKey;
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("idToken", idToken);
            
            LOG.infof("🗑️ Deleting user from Identity Platform");
            
            Response response = client.target(requestUrl)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(payload));
            
            if (response.getStatus() == 200) {
                LOG.infof("✅ User deleted successfully from Identity Platform");
            } else {
                String errorBody = response.readEntity(String.class);
                LOG.warnf("⚠️ Failed to delete user from Identity Platform. Status: %d, Body: %s", 
                    response.getStatus(), errorBody);
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "❌ Error deleting user from Identity Platform: %s", e.getMessage());
            // Don't throw - this is a cleanup operation
            
        } finally {
            client.close();
        }
    }
}
