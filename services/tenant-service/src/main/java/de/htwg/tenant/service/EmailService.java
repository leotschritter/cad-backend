package de.htwg.tenant.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Service for sending email notifications.
 */
@ApplicationScoped
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class);

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "tenant.base-domain")
    String baseDomain;

    /**
     * Send tenant activation email to the owner.
     */
    public void sendTenantActivationEmail(String ownerEmail, String tenantName, String subdomain, String tenantId) {
        try {
            String subject = String.format("🎉 Your Tenant '%s' is Ready!", tenantName);
            String body = buildActivationEmailBody(tenantName, subdomain, tenantId, ownerEmail);

            LOG.infof("📨 Sending tenant activation email to: %s", ownerEmail);
            mailer.send(Mail.withHtml(ownerEmail, subject, body));
            LOG.infof("✅ Activation email sent successfully to: %s", ownerEmail);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to send activation email to: %s", ownerEmail);
        }
    }

    /**
     * Send tenant provisioning failed email to the owner.
     */
    public void sendTenantProvisioningFailedEmail(String ownerEmail, String tenantName, String errorMessage) {
        try {
            String subject = String.format("⚠️ Tenant Provisioning Failed: '%s'", tenantName);
            String body = buildFailureEmailBody(tenantName, errorMessage);

            LOG.infof("📨 Sending provisioning failure email to: %s", ownerEmail);
            mailer.send(Mail.withHtml(ownerEmail, subject, body));
            LOG.infof("✅ Failure email sent successfully to: %s", ownerEmail);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to send failure email to: %s", ownerEmail);
        }
    }

    /**
     * Send tenant deletion confirmation email to the owner.
     */
    public void sendTenantDeletionEmail(String ownerEmail, String tenantName) {
        try {
            String subject = String.format("Tenant Deleted: '%s'", tenantName);
            String body = buildDeletionEmailBody(tenantName);

            LOG.infof("📨 Sending tenant deletion email to: %s", ownerEmail);
            mailer.send(Mail.withHtml(ownerEmail, subject, body));
            LOG.infof("✅ Deletion email sent successfully to: %s", ownerEmail);

        } catch (Exception e) {
            LOG.errorf(e, "❌ Failed to send deletion email to: %s", ownerEmail);
        }
    }

    private String buildActivationEmailBody(String tenantName, String subdomain, String tenantId, String ownerEmail) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                    .info-box { background-color: white; padding: 15px; margin: 15px 0; border-left: 4px solid #4CAF50; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    a { color: #4CAF50; text-decoration: none; }
                    .button { display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Your Tenant is Ready!</h1>
                    </div>
                    <div class="content">
                        <p>Hello,</p>
                        <p>Great news! Your tenant <strong>%s</strong> has been successfully provisioned and is now active.</p>
                        
                        <div class="info-box">
                            <h3>📋 Tenant Details</h3>
                            <p><strong>Tenant Name:</strong> %s</p>
                            <p><strong>Tenant ID:</strong> %s</p>
                            <p><strong>Frontend URL:</strong> <a href="https://%s">https://%s</a></p>
                            <p><strong>Owner Email:</strong> %s</p>
                        </div>
                        
                        <p>You can now access your tenant and start using the platform!</p>
                        
                        <a href="https://%s" class="button">Access Your Tenant</a>
                        
                        <p style="margin-top: 30px;">If you have any questions or need assistance, please don't hesitate to contact our support team.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from the Tenant Management System</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            tenantName, tenantName, tenantId, subdomain, subdomain, ownerEmail, subdomain
        );
    }

    private String buildFailureEmailBody(String tenantName, String errorMessage) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f44336; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                    .error-box { background-color: white; padding: 15px; margin: 15px 0; border-left: 4px solid #f44336; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>⚠️ Tenant Provisioning Failed</h1>
                    </div>
                    <div class="content">
                        <p>Hello,</p>
                        <p>Unfortunately, we encountered an issue while provisioning your tenant <strong>%s</strong>.</p>
                        
                        <div class="error-box">
                            <h3>❌ Error Details</h3>
                            <p>%s</p>
                        </div>
                        
                        <p>Our team has been notified and will investigate the issue. Please contact support if you need immediate assistance.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from the Tenant Management System</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            tenantName, errorMessage != null ? errorMessage : "Unknown error occurred"
        );
    }

    private String buildDeletionEmailBody(String tenantName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 30px; border-radius: 0 0 5px 5px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Tenant Deleted</h1>
                    </div>
                    <div class="content">
                        <p>Hello,</p>
                        <p>Your tenant <strong>%s</strong> has been successfully deleted.</p>
                        <p>All associated resources have been removed from our system.</p>
                        <p>Thank you for using our platform!</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from the Tenant Management System</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            tenantName
        );
    }
}

