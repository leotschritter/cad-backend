package de.htwg.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
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
    @ConfigProperty(name = "firestore.use-emulator", defaultValue = "false")
    boolean useEmulator;

    @Inject
    @ConfigProperty(name = "firestore.emulator-host", defaultValue = "localhost:8200")
    String emulatorHost;

    private static final Logger log = LoggerFactory.getLogger(FirestoreConfig.class);

    @Produces
    @ApplicationScoped
    public Firestore firestore() throws IOException {
        FirestoreOptions.Builder firestoreOptionsBuilder = FirestoreOptions.newBuilder()
                .setProjectId(projectId);

        if (useEmulator) {
            // Use emulator settings
            firestoreOptionsBuilder.setEmulatorHost(emulatorHost);
            firestoreOptionsBuilder.setCredentials(NoCredentials.getInstance());
            log.info("Using Firestore Emulator at " + emulatorHost);
        } else {
            log.info("Connect to Firestore in Google Cloud for project " + projectId);
            // Use actual Firestore credentials
            firestoreOptionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
        }

        return firestoreOptionsBuilder.build().getService();
    }
}
