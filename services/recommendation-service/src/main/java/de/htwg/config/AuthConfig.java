package de.htwg.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;

/**
 * Configuration class for initializing Google Cloud Identity Platform.
 * 
 * Google Cloud Identity Platform is Google's CIAM (Customer Identity and Access Management) solution.
 * It provides:
 * - Authentication as a service
 * - Broad protocol support (OAuth, SAML, OpenID Connect)
 * - Multi-tenancy support
 * - Intelligent account protection
 * - Enterprise support and SLA
 * 
 * Note: This uses Firebase Admin SDK, which is the official SDK for Identity Platform.
 * Firebase Authentication and Identity Platform are the same backend service.
 */
@ApplicationScoped
@Startup
public class AuthConfig {

    private static final Logger LOG = Logger.getLogger(AuthConfig.class);

    @Inject
    @ConfigProperty(name = "google.cloud.projectId", defaultValue = "")
    String projectId;

    @Inject
    @ConfigProperty(name = "identity-platform.auth.enabled", defaultValue = "true")
    boolean authEnabled;

    @PostConstruct
    public void init() {
        if (!authEnabled) {
            LOG.info("Google Cloud Identity Platform authentication is disabled");
            return;
        }

        try {
            // Check if Firebase app is already initialized
            // (Firebase Admin SDK is the official SDK for Identity Platform)
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault());
                
                // Only set project ID if provided
                if (projectId != null && !projectId.isEmpty()) {
                    optionsBuilder.setProjectId(projectId);
                }
                
                FirebaseApp.initializeApp(optionsBuilder.build());
                LOG.info("Google Cloud Identity Platform initialized successfully" + 
                        (projectId != null && !projectId.isEmpty() ? " for project: " + projectId : ""));
                LOG.info("Using Firebase Admin SDK (official SDK for Identity Platform)");
            } else {
                LOG.info("Identity Platform already initialized");
            }
        } catch (IOException e) {
            LOG.error("Failed to initialize Google Cloud Identity Platform. Make sure GOOGLE_APPLICATION_CREDENTIALS is set.", e);
            throw new RuntimeException("Failed to initialize Identity Platform", e);
        }
    }
}

