package de.htwg.travelwarnings.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Entity representing a user's trip itinerary.
 * This is a simplified version that would normally be synced from the Itinerary Service.
 * For this implementation, we'll expose endpoints to manage these for demonstration.
 */
@Entity
@Table(name = "user_trips", indexes = {
    @Index(name = "idx_ut_email", columnList = "email"),
    @Index(name = "idx_ut_country_code", columnList = "countryCode"),
    @Index(name = "idx_ut_travel_dates", columnList = "startDate,endDate")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String countryCode;

    @Column(nullable = false)
    private String countryName;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String tripName;

    @Column(nullable = false)
    private Boolean notificationsEnabled = true;

    /**
     * Check if the trip is currently active
     */
    @Transient
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    /**
     * Check if the trip is upcoming (starts in the future)
     */
    @Transient
    public boolean isUpcoming() {
        return LocalDate.now().isBefore(startDate);
    }

    /**
     * Check if the trip overlaps with a given date range
     */
    public boolean overlapsWithDates(LocalDate checkStart, LocalDate checkEnd) {
        return !endDate.isBefore(checkStart) && !startDate.isAfter(checkEnd);
    }
}

