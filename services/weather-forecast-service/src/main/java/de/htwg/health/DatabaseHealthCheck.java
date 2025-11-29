package de.htwg.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import javax.sql.DataSource;
import java.sql.Connection;

@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

    @Inject
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);
            return HealthCheckResponse.named("Database connection health check")
                    .status(isValid)
                    .build();
        } catch (Exception e) {
            return HealthCheckResponse.named("Database connection health check")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
