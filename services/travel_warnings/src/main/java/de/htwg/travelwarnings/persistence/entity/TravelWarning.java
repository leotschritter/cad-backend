package de.htwg.travelwarnings.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a travel warning from Auswärtiges Amt.
 * Stores comprehensive safety information for a country.
 */
@Entity
@Table(name = "travel_warnings", indexes = {
    @Index(name = "idx_tw_country_code", columnList = "countryCode"),
    @Index(name = "idx_tw_content_id", columnList = "contentId"),
    @Index(name = "idx_tw_last_modified", columnList = "lastModified")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String contentId;

    @Column(nullable = false)
    private Long lastModified;

    @Column(nullable = false)
    private Long effective;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(length = 3)
    private String iso3CountryCode;

    @Column(nullable = false)
    private String countryName;

    @Column(nullable = false)
    private Boolean warning;

    @Column(nullable = false)
    private Boolean partialWarning;

    @Column(nullable = false)
    private Boolean situationWarning;

    @Column(nullable = false)
    private Boolean situationPartWarning;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Instant fetchedAt;

    /**
     * Calculate the severity level based on warning flags
     */
    @Transient
    public WarningSeverity getSeverity() {
        if (Boolean.TRUE.equals(warning)) {
            return WarningSeverity.CRITICAL;
        } else if (Boolean.TRUE.equals(partialWarning)) {
            return WarningSeverity.SEVERE;
        } else if (Boolean.TRUE.equals(situationWarning)) {
            return WarningSeverity.MODERATE;
        } else if (Boolean.TRUE.equals(situationPartWarning)) {
            return WarningSeverity.MINOR;
        }
        return WarningSeverity.NONE;
    }

    /**
     * Check if this warning has any active alerts
     */
    @Transient
    public boolean hasActiveWarning() {
        return Boolean.TRUE.equals(warning) ||
               Boolean.TRUE.equals(partialWarning) ||
               Boolean.TRUE.equals(situationWarning) ||
               Boolean.TRUE.equals(situationPartWarning);
    }
}

