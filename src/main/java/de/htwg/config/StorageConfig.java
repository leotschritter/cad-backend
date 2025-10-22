package de.htwg.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ApplicationScoped
public class StorageConfig {

    @Inject
    @ConfigProperty(name = "google.cloud.projectId")
    String projectId;

    @Inject
    @ConfigProperty(name = "storage.use-emulator", defaultValue = "false")
    boolean useEmulator;

    @Inject
    @ConfigProperty(name = "storage.emulator-host", defaultValue = "localhost:9023")
    String emulatorHost;

    @Inject
    @ConfigProperty(name = "storage.bucket-name", defaultValue = "tripico-comments")
    String bucketName;

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Produces
    @ApplicationScoped
    public Storage storage() throws IOException {
        StorageOptions.Builder storageOptionsBuilder = StorageOptions.newBuilder()
                .setProjectId(projectId);

        if (useEmulator) {
            // Use emulator settings
            storageOptionsBuilder.setHost("http://" + emulatorHost);
            log.info("Using Storage Emulator at " + emulatorHost);
        } else {
            // Use actual Storage credentials
            log.info("Using Cloud Storage at " + projectId);
            storageOptionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
        }

        return storageOptionsBuilder.build().getService();
    }


}
