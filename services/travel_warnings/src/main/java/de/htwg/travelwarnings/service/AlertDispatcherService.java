package de.htwg.travelwarnings.service;

import de.htwg.travelwarnings.persistence.entity.TravelWarning;
import de.htwg.travelwarnings.persistence.entity.UserTrip;
import de.htwg.travelwarnings.persistence.entity.WarningNotification;
import de.htwg.travelwarnings.persistence.repository.WarningNotificationRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service for dispatching travel warning alerts to users.
 * Implements the "Alert Dispatcher" component from the bounded context diagram.
 *
 * User Story 2: Clear, concise summary of what changed and how it impacts the user
 */
@ApplicationScoped
public class AlertDispatcherService {

    private static final Logger LOG = Logger.getLogger(AlertDispatcherService.class);

    @Inject
    Mailer mailer;

    @Inject
    WarningNotificationRepository notificationRepository;

    @Inject
    WarningContentService contentService;

    /**
     * Send an alert email for a travel warning to a user.
     * Only sends if notification hasn't been sent for this warning version.
     */
    @Transactional
    public boolean sendAlert(TravelWarning warning, UserTrip trip, String userEmail) {

        // Check if we already sent a notification for this warning version
        if (notificationRepository.wasNotificationSentRecently(
                trip.getId(), warning.getContentId(), warning.getLastModified())) {
            LOG.debugf("Notification already sent for trip %d and warning %s (version %d)",
                      trip.getId(), warning.getContentId(), warning.getLastModified());
            return false;
        }

        try {
            String subject = buildEmailSubject(warning, trip);
            String body = buildEmailBody(warning, trip);

            mailer.send(Mail.withHtml(userEmail, subject, body));

            // Record the notification
            recordNotification(warning, trip, true, null);

            LOG.infof("Sent alert email to %s for trip to %s", userEmail, warning.getCountryName());
            return true;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to send alert email to %s", userEmail);
            recordNotification(warning, trip, false, e.getMessage());
            return false;
        }
    }

    /**
     * Build email subject line - User Story 1: Alert severity is clearly indicated
     */
    private String buildEmailSubject(TravelWarning warning, UserTrip trip) {
        String severity = warning.getSeverity().getDisplayName().toUpperCase();
        return String.format("[%s] Travel Alert: %s - %s",
                           severity,
                           warning.getCountryName(),
                           trip.getTripName());
    }

    /**
     * Build email body - User Story 2: Summary highlights what changed, when, severity, and actions
     */
    private String buildEmailBody(TravelWarning warning, UserTrip trip) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html><head><style>")
            .append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }")
            .append(".header { background-color: ").append(getSeverityColor(warning)).append("; color: white; padding: 20px; }")
            .append(".content { padding: 20px; }")
            .append(".section { margin-bottom: 20px; }")
            .append(".section-title { font-weight: bold; color: #2c3e50; margin-bottom: 10px; }")
            .append(".info-box { background-color: #f8f9fa; padding: 15px; border-left: 4px solid ").append(getSeverityColor(warning)).append("; }")
            .append("ul { margin: 10px 0; padding-left: 20px; }")
            .append(".footer { color: #666; font-size: 0.9em; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; }")
            .append("</style></head><body>");

        // Header
        html.append("<div class='header'>")
            .append("<h1>").append(warning.getSeverity().getDisplayName()).append(" Travel Alert</h1>")
            .append("<h2>").append(warning.getCountryName()).append("</h2>")
            .append("</div>");

        html.append("<div class='content'>");

        // Trip Information
        html.append("<div class='section'>")
            .append("<div class='section-title'>Your Trip:</div>")
            .append("<div class='info-box'>")
            .append("<strong>").append(trip.getTripName()).append("</strong><br>")
            .append("Destination: ").append(trip.getCountryName()).append("<br>")
            .append("Travel Dates: ").append(formatDate(trip.getStartDate()))
            .append(" to ").append(formatDate(trip.getEndDate()))
            .append("</div></div>");

        // Summary - User Story 2
        html.append(contentService.generateAlertSummary(warning));

        // Warning Details
        html.append("<div class='section'>")
            .append("<div class='section-title'>Warning Status:</div>")
            .append("<ul>");

        if (Boolean.TRUE.equals(warning.getWarning())) {
            html.append("<li><strong style='color: #dc3545;'>⚠️ Full Travel Warning Active</strong></li>");
        }
        if (Boolean.TRUE.equals(warning.getPartialWarning())) {
            html.append("<li><strong style='color: #fd7e14;'>⚠️ Partial Travel Warning for Certain Regions</strong></li>");
        }
        if (Boolean.TRUE.equals(warning.getSituationWarning())) {
            html.append("<li><strong style='color: #ffc107;'>⚠️ Situation-Specific Warning (e.g., COVID-19)</strong></li>");
        }
        if (Boolean.TRUE.equals(warning.getSituationPartWarning())) {
            html.append("<li><strong style='color: #ffc107;'>⚠️ Partial Situation-Specific Warning</strong></li>");
        }

        html.append("</ul></div>");

        // Recommended Actions
        html.append("<div class='section'>")
            .append("<div class='section-title'>Recommended Actions:</div>")
            .append("<ul>")
            .append("<li>Review the full travel advisory on the Auswärtiges Amt website</li>")
            .append("<li>Check with your travel insurance provider</li>")
            .append("<li>Register with the crisis management app of the German Foreign Office</li>")
            .append("<li>Keep emergency contact numbers readily available</li>");

        if (warning.getSeverity().getLevel() >= 3) {
            html.append("<li><strong>Consider postponing or canceling your trip</strong></li>");
        }

        html.append("</ul></div>");

        // Footer
        html.append("<div class='footer'>")
            .append("<p>Last updated: ").append(formatTimestamp(warning.getLastModified())).append("</p>")
            .append("<p>This is an automated alert from your Travel SaaS Platform. ")
            .append("For the most current information, visit the official ")
            .append("<a href='https://www.auswaertiges-amt.de/'>Auswärtiges Amt website</a>.</p>")
            .append("<p><small>To manage your notification preferences, log in to your account.</small></p>")
            .append("</div>");

        html.append("</div></body></html>");

        return html.toString();
    }

    /**
     * Get color based on severity
     */
    private String getSeverityColor(TravelWarning warning) {
        return switch (warning.getSeverity()) {
            case CRITICAL -> "#dc3545";  // Red
            case SEVERE -> "#fd7e14";    // Orange
            case MODERATE -> "#ffc107";  // Yellow
            case MINOR -> "#17a2b8";     // Blue
            default -> "#6c757d";        // Gray
        };
    }

    /**
     * Record notification in database
     */
    private void recordNotification(TravelWarning warning, UserTrip trip,
                                    boolean successful, String errorMessage) {
        WarningNotification notification = new WarningNotification();
        notification.setUserTripId(trip.getId());
        notification.setEmail(trip.getEmail());
        notification.setWarningContentId(warning.getContentId());
        notification.setCountryCode(warning.getCountryCode());
        notification.setCountryName(warning.getCountryName());
        notification.setSeverity(warning.getSeverity());
        notification.setSentAt(Instant.now());
        notification.setSuccessful(successful);
        notification.setErrorMessage(errorMessage);
        notification.setWarningLastModified(warning.getLastModified());

        notificationRepository.persist(notification);
    }

    /**
     * Format date for display
     */
    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }

    /**
     * Format timestamp for display
     */
    private String formatTimestamp(Long timestamp) {
        if (timestamp == null) return "Unknown";
        return Instant.ofEpochMilli(timestamp)
                     .toString()
                     .replace('T', ' ')
                     .substring(0, 19);
    }
}

