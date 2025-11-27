package de.htwg.travelwarnings.client;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Determines which AuswaertigesAmtClient implementation to use.
 *
 * Configured via application.properties:
 * - app.mock-external-api=true  → MockAuswaertigesAmtClient
 * - app.mock-external-api=false → Real REST client (will fail due to bot protection)
 */
@ApplicationScoped
public class AuswaertigesAmtClientProducer {

    private static final Logger LOG = Logger.getLogger(AuswaertigesAmtClientProducer.class);

    @ConfigProperty(name = "app.mock-external-api", defaultValue = "true")
    boolean mockEnabled;

    @PostConstruct
    void init() {
        if (mockEnabled) {
            LOG.warn("═".repeat(80));
            LOG.warn("MOCK MODE ENABLED - Using mock travel warning data");
            LOG.warn("Real Auswärtiges Amt API is blocked by bot protection (Enodia)");
            LOG.warn("Set app.mock-external-api=false to attempt real API calls");
            LOG.warn("═".repeat(80));
        } else {
            LOG.info("REAL API MODE - Attempting to connect to Auswärtiges Amt API");
            LOG.info("Note: This will likely fail due to Enodia bot protection");
        }
    }
}

