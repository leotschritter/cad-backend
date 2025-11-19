package de.htwg.travelwarnings.persistence.repository;

import de.htwg.travelwarnings.persistence.entity.UserTrip;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for UserTrip entities
 */
@ApplicationScoped
public class UserTripRepository implements PanacheRepository<UserTrip> {

    public List<UserTrip> findByEmail(String email) {
        return list("email", email);
    }

    public List<UserTrip> findActiveAndUpcomingTrips() {
        LocalDate today = LocalDate.now();
        return list("endDate >= ?1", today);
    }

    public List<UserTrip> findByEmailAndActiveOrUpcoming(String email) {
        LocalDate today = LocalDate.now();
        return list("email = ?1 AND endDate >= ?2", email, today);
    }

    public List<UserTrip> findByCountryCode(String countryCode) {
        return list("countryCode", countryCode);
    }

    public List<UserTrip> findByCountryCodeAndActiveOrUpcoming(String countryCode) {
        LocalDate today = LocalDate.now();
        return list("countryCode = ?1 AND endDate >= ?2", countryCode, today);
    }

    public List<UserTrip> findByCountryCodesAndActiveOrUpcoming(List<String> countryCodes) {
        LocalDate today = LocalDate.now();
        return list("countryCode in ?1 AND endDate >= ?2 AND notificationsEnabled = true",
                    countryCodes, today);
    }
}

