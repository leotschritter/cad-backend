package de.htwg.travelwarnings.persistence.repository;

import de.htwg.travelwarnings.persistence.entity.TravelWarning;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TravelWarning entities
 */
@ApplicationScoped
public class TravelWarningRepository implements PanacheRepository<TravelWarning> {

    public Optional<TravelWarning> findByContentId(String contentId) {
        return find("contentId", contentId).firstResultOptional();
    }

    public Optional<TravelWarning> findByCountryCode(String countryCode) {
        return find("countryCode", countryCode).firstResultOptional();
    }

    public List<TravelWarning> findByCountryCodes(List<String> countryCodes) {
        return list("countryCode in ?1", countryCodes);
    }

    public List<TravelWarning> findAllWithActiveWarnings() {
        return list("warning = true OR partialWarning = true OR situationWarning = true OR situationPartWarning = true");
    }

    public List<TravelWarning> findActiveWarningsByCountryCodes(List<String> countryCodes) {
        return list("countryCode in ?1 AND (warning = true OR partialWarning = true OR situationWarning = true OR situationPartWarning = true)",
                    countryCodes);
    }
}

