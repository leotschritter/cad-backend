package de.htwg.travelwarnings.persistence.repository;

import de.htwg.travelwarnings.persistence.entity.WarningNotification;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WarningNotification entities
 */
@ApplicationScoped
public class WarningNotificationRepository implements PanacheRepository<WarningNotification> {

    public List<WarningNotification> findByUserTripId(Long userTripId) {
        return list("userTripId", userTripId);
    }

    public List<WarningNotification> findByEmail(String email) {
        return list("email", email);
    }

    public Optional<WarningNotification> findLastNotificationForTripAndWarning(Long userTripId, String warningContentId) {
        return find("userTripId = ?1 AND warningContentId = ?2 ORDER BY sentAt DESC",
                    userTripId, warningContentId)
                .firstResultOptional();
    }

    public boolean wasNotificationSentRecently(Long userTripId, String warningContentId, Long warningLastModified) {
        return find("userTripId = ?1 AND warningContentId = ?2 AND warningLastModified = ?3 AND successful = true",
                    userTripId, warningContentId, warningLastModified)
                .firstResultOptional()
                .isPresent();
    }

    public List<WarningNotification> findRecentNotifications(String email, int days) {
        Instant cutoff = Instant.now().minusSeconds(days * 24 * 60 * 60);
        return list("email = ?1 AND sentAt >= ?2 ORDER BY sentAt DESC", email, cutoff);
    }
}

