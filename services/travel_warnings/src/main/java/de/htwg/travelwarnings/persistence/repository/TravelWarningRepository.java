package de.htwg.travelwarnings.persistence.repository;

import de.htwg.travelwarnings.persistence.entity.TravelWarning;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TravelWarning entities
 */
@ApplicationScoped
public class TravelWarningRepository implements PanacheRepository<TravelWarning> {

    private static final Logger LOG = Logger.getLogger(TravelWarningRepository.class);

    public Optional<TravelWarning> findByContentId(String contentId) {
        LOG.debugf("Finding travel warning by contentId: %s", contentId);
        Optional<TravelWarning> result = find("contentId", contentId).firstResultOptional();
        LOG.debugf("Found travel warning by contentId %s: %s", contentId, result.isPresent());
        return result;
    }

    public Optional<TravelWarning> findByCountryCode(String countryCode) {
        LOG.debugf("Finding travel warning by countryCode: %s", countryCode);
        Optional<TravelWarning> result = find("countryCode", countryCode).firstResultOptional();
        if (result.isPresent()) {
            LOG.debugf("Found travel warning for country %s with severity: %s",
                      countryCode, result.get().getSeverity());
        } else {
            LOG.debugf("No travel warning found for country: %s", countryCode);
        }
        return result;
    }

    public List<TravelWarning> findByCountryCodes(List<String> countryCodes) {
        LOG.debugf("Finding travel warnings for %d country codes: %s", countryCodes.size(), countryCodes);
        List<TravelWarning> results = list("countryCode in ?1", countryCodes);
        LOG.debugf("Found %d travel warnings for the requested country codes", results.size());
        return results;
    }

    public List<TravelWarning> findAllWithActiveWarnings() {
        LOG.debug("Finding all travel warnings with active warning flags");
        List<TravelWarning> results = list("warning = true OR partialWarning = true OR situationWarning = true OR situationPartWarning = true");
        LOG.debugf("Found %d travel warnings with active warning flags", results.size());
        return results;
    }

    public List<TravelWarning> findActiveWarningsByCountryCodes(List<String> countryCodes) {
        LOG.debugf("Finding active travel warnings for %d country codes: %s", countryCodes.size(), countryCodes);
        List<TravelWarning> results = list("countryCode in ?1 AND (warning = true OR partialWarning = true OR situationWarning = true OR situationPartWarning = true)",
                    countryCodes);
        LOG.debugf("Found %d active travel warnings for the requested country codes", results.size());
        return results;
    }
}

