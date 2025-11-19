package de.htwg.travelwarnings.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Health check for the travel warnings service
 * Note: Database connectivity is checked by Quarkus's built-in DataSource health check
 */
@Readiness
@ApplicationScoped
public class TravelWarningsHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        // Simple health check without database query
        // Database connectivity is already checked by Quarkus's DataSource health check
        return HealthCheckResponse.named("Travel Warnings Service")
            .up()
            .withData("status", "Service is running")
            .build();
    }
}

