package de.htwg.travelwarnings.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a notification sent to a user about a travel warning.
 * Used to track alert history and prevent duplicate notifications.
 */
@Entity
@Table(name = "warning_notifications", indexes = {
    @Index(name = "idx_wn_user_trip", columnList = "userTripId"),
    @Index(name = "idx_wn_warning", columnList = "warningContentId"),
    @Index(name = "idx_wn_sent_at", columnList = "sentAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarningNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userTripId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String warningContentId;

    @Column(nullable = false)
    private String countryCode;

    @Column(nullable = false)
    private String countryName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WarningSeverity severity;

    @Column(nullable = false)
    private Instant sentAt;

    @Column(nullable = false)
    private Boolean successful;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private Long warningLastModified;
}

