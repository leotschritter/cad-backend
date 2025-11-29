package de.htwg.travelwarnings.service;

import de.htwg.travelwarnings.persistence.entity.TravelWarning;
import de.htwg.travelwarnings.persistence.entity.UserTrip;
import de.htwg.travelwarnings.persistence.repository.TravelWarningRepository;
import de.htwg.travelwarnings.persistence.repository.UserTripRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for matching travel warnings with user trips.
 * Implements the "Warning Matcher" component from the bounded context diagram.
 *
 * User Story 1: Alerts are triggered only for countries in my itinerary
 * and notifications are sent only when warnings overlap with my travel timeframe.
 */
@ApplicationScoped
public class WarningMatcherService {

    private static final Logger LOG = Logger.getLogger(WarningMatcherService.class);

    @Inject
    UserTripRepository userTripRepository;

    @Inject
    TravelWarningRepository travelWarningRepository;

    /**
     * Find all user trips that are affected by the given travel warning.
     * Only returns trips that:
     * 1. Match the country code
     * 2. Are currently active or upcoming (not past)
     * 3. Have notifications enabled
     */
    public List<UserTrip> findAffectedTrips(TravelWarning warning) {
        if (warning == null || !warning.hasActiveWarning()) {
            return List.of();
        }

        List<UserTrip> trips = userTripRepository.findByCountryCodeAndActiveOrUpcoming(warning.getCountryCode());

        // Filter to only include trips with notifications enabled
        List<UserTrip> affectedTrips = trips.stream()
            .filter(UserTrip::getNotificationsEnabled)
            .collect(Collectors.toList());

        LOG.debugf("Found %d affected trips for warning in %s", affectedTrips.size(), warning.getCountryName());

        return affectedTrips;
    }

    /**
     * Find all active warnings that affect the given user's trips
     */
    public List<WarningMatch> findWarningsForUser(String email) {
        List<UserTrip> trips = userTripRepository.findByEmailAndActiveOrUpcoming(email);

        if (trips.isEmpty()) {
            return List.of();
        }

        List<String> countryCodes = trips.stream()
            .map(UserTrip::getCountryCode)
            .distinct()
            .collect(Collectors.toList());

        List<TravelWarning> warnings = travelWarningRepository.findActiveWarningsByCountryCodes(countryCodes);

        return matchWarningsToTrips(warnings, trips);
    }

    /**
     * Find all warnings that affect active/upcoming trips
     */
    public List<WarningMatch> findAllActiveWarningMatches() {
        List<TravelWarning> warnings = travelWarningRepository.findAllWithActiveWarnings();

        if (warnings.isEmpty()) {
            return List.of();
        }

        List<String> countryCodes = warnings.stream()
            .map(TravelWarning::getCountryCode)
            .distinct()
            .collect(Collectors.toList());

        List<UserTrip> trips = userTripRepository.findByCountryCodesAndActiveOrUpcoming(countryCodes);

        return matchWarningsToTrips(warnings, trips);
    }

    /**
     * Match warnings to trips and create WarningMatch objects
     */
    private List<WarningMatch> matchWarningsToTrips(List<TravelWarning> warnings, List<UserTrip> trips) {
        List<WarningMatch> matches = new ArrayList<>();

        for (TravelWarning warning : warnings) {
            for (UserTrip trip : trips) {
                if (trip.getCountryCode().equals(warning.getCountryCode()) &&
                    trip.getNotificationsEnabled() &&
                    (trip.isActive() || trip.isUpcoming())) {
                    matches.add(new WarningMatch(warning, trip));
                }
            }
        }

        LOG.debugf("Created %d warning-trip matches", matches.size());
        return matches;
    }

    /**
     * Check if a trip overlaps with the current date (used for immediate alerts)
     */
    public boolean isTripRelevantNow(UserTrip trip) {
        LocalDate today = LocalDate.now();
        return trip.isActive() ||
               (trip.isUpcoming() && trip.getStartDate().minusDays(7).isBefore(today));
    }

    /**
     * Value object representing a match between a warning and a trip
     */
    public static class WarningMatch {
        private final TravelWarning warning;
        private final UserTrip trip;

        public WarningMatch(TravelWarning warning, UserTrip trip) {
            this.warning = warning;
            this.trip = trip;
        }

        public TravelWarning getWarning() {
            return warning;
        }

        public UserTrip getTrip() {
            return trip;
        }
    }
}

