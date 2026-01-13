package de.htwg.tenant.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.TenantAwareFirebaseAuth;
import com.google.firebase.auth.UserRecord;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Service for managing Identity Platform tenants and users.
 * Note: Tenant creation is handled by the GitHub workflow.
 * This service manages user creation within tenants after provisioning.
 */
@ApplicationScoped
public class IdentityPlatformService {

    private static final Logger LOG = Logger.getLogger(IdentityPlatformService.class);

    /**
     * Create a user in a specific Identity Platform tenant.
     * This should be called after the tenant is provisioned by the workflow.
     * 
     * @param tenantId The Identity Platform tenant ID (returned from gcloud)
     * @param email User email
     * @param password User password
     * @return The created user's UID
     */
    public String createUserInTenant(String tenantId, String email, String password) {
        try {
            // Get tenant-aware FirebaseAuth instance
            TenantAwareFirebaseAuth tenantAuth = FirebaseAuth.getInstance().getTenantManager()
                .getAuthForTenant(tenantId);

            // Create user
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setEmailVerified(false); // User should verify their email

            UserRecord userRecord = tenantAuth.createUser(request);
            
            LOG.infof("✅ Created user %s in tenant %s (UID: %s)", 
                email, tenantId, userRecord.getUid());

            return userRecord.getUid();

        } catch (FirebaseAuthException e) {
            LOG.errorf(e, "❌ Failed to create user %s in tenant %s", email, tenantId);
            throw new RuntimeException("Failed to create user in Identity Platform tenant: " + e.getMessage(), e);
        }
    }

    /**
     * Send email verification to a user in a tenant.
     * 
     * @param tenantId The Identity Platform tenant ID
     * @param email User email
     */
    public void sendEmailVerification(String tenantId, String email) {
        try {
            TenantAwareFirebaseAuth tenantAuth = FirebaseAuth.getInstance().getTenantManager()
                .getAuthForTenant(tenantId);

            UserRecord user = tenantAuth.getUserByEmail(email);
            
            // Generate email verification link
            String link = tenantAuth.generateEmailVerificationLink(email);
            
            LOG.infof("📧 Email verification link generated for %s in tenant %s", email, tenantId);
            // Note: The actual email sending should be integrated with EmailService
            // For now, just log the link

        } catch (FirebaseAuthException e) {
            LOG.errorf(e, "❌ Failed to send email verification to %s in tenant %s", email, tenantId);
        }
    }

    /**
     * Delete a user from a tenant.
     * Called during tenant deprovisioning.
     * 
     * @param tenantId The Identity Platform tenant ID
     * @param uid User UID
     */
    public void deleteUser(String tenantId, String uid) {
        try {
            TenantAwareFirebaseAuth tenantAuth = FirebaseAuth.getInstance().getTenantManager()
                .getAuthForTenant(tenantId);

            tenantAuth.deleteUser(uid);
            
            LOG.infof("✅ Deleted user %s from tenant %s", uid, tenantId);

        } catch (FirebaseAuthException e) {
            LOG.errorf(e, "❌ Failed to delete user %s from tenant %s", uid, tenantId);
        }
    }

    /**
     * Check if a user exists in a tenant.
     * 
     * @param tenantId The Identity Platform tenant ID
     * @param email User email
     * @return true if user exists
     */
    public boolean userExists(String tenantId, String email) {
        try {
            TenantAwareFirebaseAuth tenantAuth = FirebaseAuth.getInstance().getTenantManager()
                .getAuthForTenant(tenantId);

            tenantAuth.getUserByEmail(email);
            return true;

        } catch (FirebaseAuthException e) {
            if (e.getMessage().contains("USER_NOT_FOUND")) {
                return false;
            }
            LOG.errorf(e, "Error checking if user exists: %s", email);
            return false;
        }
    }
}

