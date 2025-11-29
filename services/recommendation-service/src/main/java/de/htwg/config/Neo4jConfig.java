package de.htwg.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

/**
 * Configuration class for Neo4j Driver.
 * Creates and provides a CDI-managed Neo4j Driver instance.
 */
@ApplicationScoped
public class Neo4jConfig {

    private static final Logger LOG = Logger.getLogger(Neo4jConfig.class);

    @ConfigProperty(name = "quarkus.neo4j.uri")
    String uri;

    @ConfigProperty(name = "quarkus.neo4j.authentication.username")
    String username;

    @ConfigProperty(name = "quarkus.neo4j.authentication.password")
    String password;

    @Produces
    @ApplicationScoped
    public Driver createDriver() {
        LOG.infof("Creating Neo4j Driver for URI: %s", uri);
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }

    public void closeDriver(Driver driver) {
        if (driver != null) {
            LOG.info("Closing Neo4j Driver");
            driver.close();
        }
    }
}

