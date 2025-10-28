package de.htwg.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PreDestroy;
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
    @ConfigProperty(name = "google.storage.use-emulator", defaultValue = "false")
    boolean useEmulator;

    @Inject
    @ConfigProperty(name = "google.storage.emulator-host", defaultValue = "localhost:9023")
    String emulatorHost;

    @Inject
    @ConfigProperty(name = "google.storage.bucket-name", defaultValue = "tripico-comments")
    String bucketName;

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);
    
    private Storage storageInstance;

    @Produces
    @ApplicationScoped
    public Storage storage() throws IOException {
        StorageOptions.Builder storageOptionsBuilder = StorageOptions.newBuilder()
                .setProjectId(projectId);

        if (useEmulator) {
            // Use emulator settings
            storageOptionsBuilder.setHost("http://" + emulatorHost);
            log.info("Using Storage Emulator at {}", emulatorHost);
        } else {
            // Use actual Storage credentials
            log.info("Connecting to Cloud Storage for project: {}", projectId);
            try {
                GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
                storageOptionsBuilder.setCredentials(credentials);
                log.info("Application Default Credentials loaded successfully");
            } catch (IOException e) {
                log.error("❌ Failed to load Application Default Credentials", e);
                throw e;
            }
        }

        try {
            storageInstance = storageOptionsBuilder.build().getService();
            log.info("Storage instance created successfully for project: {} with bucket: {}", projectId, bucketName);
        } catch (Exception e) {
            log.error("Failed to create Storage instance", e);
            throw e;
        }
        
        return storageInstance;
    }
    
    @PreDestroy
    public void cleanup() {
        if (storageInstance != null) {
            try {
                storageInstance.close();
                log.info("Storage connection closed successfully");
            } catch (Exception e) {
                log.error("Error closing Storage connection", e);
            }
        }
    }

}
