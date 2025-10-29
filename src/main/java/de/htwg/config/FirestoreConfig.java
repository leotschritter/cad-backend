package de.htwg.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ApplicationScoped
public class FirestoreConfig {

    @Inject
    @ConfigProperty(name = "google.cloud.projectId")
    String projectId;

    @Inject
    @ConfigProperty(name = "google.firestore.use-emulator", defaultValue = "false")
    boolean useEmulator;

    @Inject
    @ConfigProperty(name = "google.firestore.emulator-host", defaultValue = "localhost:8200")
    String emulatorHost;

    private static final Logger log = LoggerFactory.getLogger(FirestoreConfig.class);
    
    private Firestore firestoreInstance;

    @Produces
    @ApplicationScoped
    public Firestore firestore() throws IOException {
        FirestoreOptions.Builder firestoreOptionsBuilder = FirestoreOptions.newBuilder()
                .setProjectId(projectId);

        if (useEmulator) {
            // Use emulator settings - must use setHost() with EmulatorCredentials
            firestoreOptionsBuilder.setHost(emulatorHost);
            firestoreOptionsBuilder.setCredentials(new FirestoreOptions.EmulatorCredentials());
            log.info("Using Firestore Emulator at {}", emulatorHost);
        } else {
            log.info("Connecting to Firestore in Google Cloud for project: {}", projectId);
            try {
                GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
                firestoreOptionsBuilder.setCredentials(credentials);
                log.info("Application Default Credentials loaded successfully");
            } catch (IOException e) {
                log.error("Failed to load Application Default Credentials", e);
                throw e;
            }
        }

        try {
            firestoreInstance = firestoreOptionsBuilder.build().getService();
            log.info("Firestore instance created successfully for project: {}", projectId);
        } catch (Exception e) {
            log.error("Failed to create Firestore instance", e);
            throw e;
        }
        
        return firestoreInstance;
    }

    @PreDestroy
    public void cleanup() {
        if (firestoreInstance != null) {
            try {
                firestoreInstance.close();
                log.info("Firestore connection closed successfully");
            } catch (Exception e) {
                log.error("Error closing Firestore connection", e);
            }
        }
    }
}
