package de.htwg.tenant.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for interacting with the Itinerary Service API.
 * Used to register users in the itinerary service database.
 */
@ApplicationScoped
public class ItineraryServiceClient {

    private static final Logger LOG = Logger.getLogger(ItineraryServiceClient.class);

    /**
     * Register a user in the itinerary service database via API Gateway.
     * 
     * @param apiGatewayUrl Base URL of the API Gateway (e.g., https://tripico-standard-16-gateway-b1kp6vxb.ew.gateway.dev)
     * @param email User's email address
     * @param name User's name (if null, will extract from email)
     * @return true if user was registered successfully, false if user already exists
     */
    public boolean registerUser(String apiGatewayUrl, String email, String name) {
        if (apiGatewayUrl == null || apiGatewayUrl.isEmpty()) {
            LOG.warnf("⚠️ API Gateway URL is not set, skipping user registration for: %s", email);
            return false;
        }

        // Use provided name or extract from email
        String userName = (name != null && !name.trim().isEmpty()) 
                ? name 
                : email.split("@")[0];

        Client client = ClientBuilder.newClient();
        
        try {
            // Build the request URL - user API is at /user via API Gateway
            String registerUrl = apiGatewayUrl;
            if (!registerUrl.endsWith("/")) {
                registerUrl += "/";
            }
            registerUrl += "user/register";
            
            // Build the request payload (UserDto format: id, name, email, profileImageUrl)
            // id and profileImageUrl are optional/null for new users
            Map<String, Object> payload = new HashMap<>();
            payload.put("email", email);
            payload.put("name", userName);
            // id and profileImageUrl are null/omitted for new registrations
            
            LOG.infof("👤 Registering user in itinerary service via API Gateway: %s (name: %s)", email, userName);
            LOG.infof("   Request URL: %s", registerUrl);
            LOG.debugf("   Request payload: %s", payload);
            
            // Send the request
            Response response = client.target(registerUrl)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(payload));
            
            int statusCode = response.getStatus();
            
            if (statusCode == 200) {
                LOG.infof("✅ User registered successfully in itinerary service: %s", email);
                return true;
                
            } else if (statusCode == 400) {
                // User might already exist - this is OK
                String errorBody = "";
                try {
                    if (response.hasEntity()) {
                        errorBody = response.readEntity(String.class);
                    }
                } catch (Exception e) {
                    LOG.warnf("Could not read error response body: %s", e.getMessage());
                }
                
                if (errorBody != null && errorBody.contains("already exists")) {
                    LOG.debugf("User already exists in itinerary service: %s (this is OK)", email);
                    return true; // Consider this success - user exists
                } else {
                    LOG.warnf("⚠️ Bad request when registering user in itinerary service. Status: %d, Body: %s", 
                        statusCode, errorBody);
                    return false;
                }
                
            } else {
                // Try to read error response
                String errorBody = "";
                try {
                    if (response.hasEntity()) {
                        errorBody = response.readEntity(String.class);
                    }
                } catch (Exception e) {
                    LOG.warnf("Could not read error response body: %s", e.getMessage());
                }
                
                LOG.errorf("❌ Failed to register user in itinerary service. Status: %d, Body: %s", 
                    statusCode, errorBody);
                LOG.errorf("   Request URL: %s", registerUrl);
                LOG.errorf("   Email: %s, Name: %s", email, userName);
                LOG.errorf("   Payload: %s", payload);
                
                // Don't throw - this is a best-effort operation
                return false;
            }
            
        } catch (Exception e) {
            LOG.warnf(e, "⚠️ Error registering user in itinerary service: %s", e.getMessage());
            // Don't throw - this is a best-effort operation
            return false;
            
        } finally {
            client.close();
        }
    }
}
