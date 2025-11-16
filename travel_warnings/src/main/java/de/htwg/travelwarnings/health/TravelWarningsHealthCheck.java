package de.htwg.travelwarnings.health;

import de.htwg.travelwarnings.persistence.repository.TravelWarningRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Health check for the travel warnings service
 */
@Readiness
@ApplicationScoped
public class TravelWarningsHealthCheck implements HealthCheck {

    @Inject
    TravelWarningRepository warningRepository;

    @Override
    public HealthCheckResponse call() {
        try {
            // Check if we can query the database
            long count = warningRepository.count();

            return HealthCheckResponse.named("Travel Warnings Service")
                .up()
                .withData("warnings_count", count)
                .build();
        } catch (Exception e) {
            return HealthCheckResponse.named("Travel Warnings Service")
                .down()
                .withData("error", e.getMessage())
                .build();
        }
    }
}

