/* Comment the file in again for quick email testing through the API */
/*
package de.htwg.travelwarnings.api.resource;

// ====== TESTING_ONLY_REMOVE_AFTER_EMAIL_VERIFICATION ======
// This resource is for testing email functionality only.
// DELETE this entire file after verifying emails work.
// ====== TESTING_ONLY_REMOVE_AFTER_EMAIL_VERIFICATION ======

import de.htwg.travelwarnings.service.AlertDispatcherService;
import de.htwg.travelwarnings.persistence.entity.TravelWarning;
import de.htwg.travelwarnings.persistence.entity.UserTrip;
import de.htwg.travelwarnings.persistence.entity.WarningSeverity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

*/
/**
 * TESTING ONLY - Email Test Endpoint
 *
 * DELETE THIS FILE AFTER EMAIL TESTING IS COMPLETE!
 *
 * Search for: TESTING_ONLY_REMOVE_AFTER_EMAIL_VERIFICATION
 *//*

@Path("/api/v1/test/email")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Testing (Remove After Email Verification)", description = "Test email functionality - DELETE after testing")
public class EmailTestResource {

    private static final Logger LOG = Logger.getLogger(EmailTestResource.class);

    @Inject
    AlertDispatcherService alertService;

    @ConfigProperty(name = "quarkus.mailer.mock", defaultValue = "true")
    boolean mailerMockMode;

    */
/**
     * TESTING_ONLY_REMOVE_AFTER_EMAIL_VERIFICATION
     *
     * Send a test email to verify email configuration is working.
     *
     * Usage:
     * POST /api/v1/test/email/send
     * Body: { "email": "your-email@example.com" }
     *//*

    @POST
    @Path("/send")
    @Operation(
        summary = "Send test email",
        description = "TESTING ONLY - Sends a test travel warning email to verify email configuration"
    )
    public Response sendTestEmail(TestEmailRequest request) {
        // TESTING_ONLY_REMOVE_AFTER_EMAIL_VERIFICATION

        LOG.info("=== EMAIL TEST STARTED ===");
        LOG.infof("Sending test email to: %s", request.email);

        if (request.email == null || request.email.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of(
                    "success", false,
                    "message", "Email address is required",
                    "example", "{ \"email\": \"your-email@example.com\" }"
                ))
                .build();
        }

        try {
            // Create mock travel warning
            TravelWarning mockWarning = createMockWarning();

            // Create mock user trip
            UserTrip mockTrip = createMockTrip(request.email);

            // Send the alert
            LOG.info("Calling AlertDispatcherService.sendAlert()...");
            boolean sent = alertService.sendAlert(mockWarning, mockTrip, request.email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", sent);
            response.put("email", request.email);
            response.put("mockMode", mailerMockMode);

            if (mailerMockMode) {
                response.put("message", sent
                    ? "✅ Email LOGGED (mock mode active). Check application logs for email content."
                    : "❌ Failed to send test email. Check application logs for details.");
                response.put("note", "Mock mode is enabled. To send real emails: Set quarkus.mailer.mock=false and configure SMTP in application.properties");
                response.put("logLocation", "Check console for: 'MOCK mode enabled' and email content");
            } else {
                response.put("message", sent
                    ? "✅ Test email sent successfully! Check your inbox."
                    : "❌ Failed to send test email. Check application logs for details.");
                response.put("note", "Real email mode is active. Email was sent via SMTP.");
            }

            response.put("testWarning", Map.of(
                "country", "Germany",
                "severity", "HIGH",
                "type", "Mock Test Warning"
            ));

            LOG.info(sent ? "✅ Test email processed successfully" : "❌ Test email failed");
            if (mailerMockMode) {
                LOG.info("📧 MOCK MODE: Email was logged, not actually sent");
                LOG.info("💡 To send real emails: Set quarkus.mailer.mock=false in application.properties");
            }
            LOG.info("=== EMAIL TEST COMPLETED ===");

            return Response.ok(response).build();

        } catch (Exception e) {
            LOG.error("❌ Email test failed with exception", e);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of(
                    "success", false,
                    "email", request.email,
                    "message", "❌ Error sending test email",
                    "error", e.getMessage(),
                    "hint", "Check application.properties for email configuration (quarkus.mailer.*)"
                ))
                .build();
        }
    }

    */
/**
     * TESTING_ONLY_REMOVE_AFTER_EMAIL_VERIFICATION
     *
     * Get email configuration status without sending an email
     *//*

    @GET
    @Path("/config")
    @Operation(
        summary = "Check email configuration",
        description = "TESTING ONLY - Shows current email configuration status"
    )
    public Response getEmailConfig() {
        // TESTING_ONLY_REMOVE_AFTER_EMAIL_VERIFICATION

        Map<String, Object> config = new HashMap<>();
        config.put("mockMode", mailerMockMode);
        config.put("endpoint", "/api/v1/test/email/send");
        config.put("method", "POST");
        config.put("body", Map.of("email", "your-email@example.com"));

        if (mailerMockMode) {
            config.put("status", "⚠️  MOCK MODE ACTIVE");
            config.put("note", "Emails are logged to console, not actually sent");
            config.put("toSendRealEmails", Map.of(
                "step1", "Edit application.properties",
                "step2", "Set: quarkus.mailer.mock=false",
                "step3", "Configure SMTP settings (see requiredProperties below)",
                "step4", "Restart application",
                "step5", "Test again"
            ));
        } else {
            config.put("status", "✅ REAL EMAIL MODE");
            config.put("note", "Emails will be sent via configured SMTP server");
        }

        config.put("requiredProperties", Map.of(
            "quarkus.mailer.mock", "false (to send real emails)",
            "quarkus.mailer.from", "noreply@example.com",
            "quarkus.mailer.host", "smtp.gmail.com (or your SMTP server)",
            "quarkus.mailer.port", "587",
            "quarkus.mailer.start-tls", "REQUIRED",
            "quarkus.mailer.username", "your-email@gmail.com",
            "quarkus.mailer.password", "your-app-password (not regular password!)"
        ));

        config.put("testCommand",
            "curl -X POST http://localhost:8080/api/v1/test/email/send -H \"Content-Type: application/json\" -d \"{\\\"email\\\":\\\"your@example.com\\\"}\"");

        return Response.ok(config).build();
    }

    // TESTING_ONLY_REMOVE_AFTER_EMAIL_VERIFICATION
    private TravelWarning createMockWarning() {
        TravelWarning warning = new TravelWarning();
        warning.setId(999L);
        warning.setContentId("TEST-001");
        warning.setLastModified(Instant.now().toEpochMilli());
        warning.setEffective(Instant.now().toEpochMilli());
        warning.setTitle("🧪 TEST EMAIL - Germany: Travel Advisory");
        warning.setCountryCode("DE");
        warning.setIso3CountryCode("DEU");
        warning.setCountryName("Germany");
        warning.setWarning(true);
        warning.setPartialWarning(false);
        warning.setSituationWarning(true);
        warning.setSituationPartWarning(false);
        warning.setContent("<h2>🧪 This is a Test Email</h2>" +
            "<p>If you received this email, your email configuration is working correctly!</p>" +
            "<p><strong>This is NOT a real travel warning.</strong></p>" +
            "<p>You can now configure real email settings and delete the test endpoint.</p>");
        warning.setFetchedAt(Instant.now());
        return warning;
    }

    // TESTING_ONLY_REMOVE_AFTER_EMAIL_VERIFICATION
    private UserTrip createMockTrip(String email) {
        UserTrip trip = new UserTrip();
        trip.setId(999L);
        trip.setEmail(email);
        trip.setCountryCode("DE");
        trip.setCountryName("Germany");
        trip.setStartDate(LocalDate.now());
        trip.setEndDate(LocalDate.now().plusDays(7));
        trip.setTripName("🧪 Test Trip - Email Verification");
        trip.setNotificationsEnabled(true);
        return trip;
    }

    // TESTING_ONLY_REMOVE_AFTER_EMAIL_VERIFICATION
    public static class TestEmailRequest {
        public String email;
    }
}

*/
