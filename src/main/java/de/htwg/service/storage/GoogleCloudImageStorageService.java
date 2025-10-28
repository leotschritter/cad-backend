package de.htwg.service.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.cloud.storage.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class GoogleCloudImageStorageService implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCloudImageStorageService.class);

    @Inject
    @ConfigProperty(name = "google.storage.bucket-name")
    String bucketName;

    @Inject
    @ConfigProperty(name = "google.cloud.projectId")
    String projectId;

    @Inject
    @ConfigProperty(name = "google.storage.use-emulator", defaultValue = "false")
    boolean useEmulator;

    @Inject
    @ConfigProperty(name = "google.storage.emulator-host", defaultValue = "localhost:9023")
    String emulatorHost;

    @ConfigProperty(name = "google.storage.service-account-email")
    Optional<String> serviceAccountEmail;

    @Inject
    Storage storage;


    @Override
    public String uploadImage(InputStream imageStream, String fileName, String contentType) {
        try {
            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();

            Blob uploadedBlob = storage.create(blobInfo, imageStream.readAllBytes());
            log.info("Successfully uploaded blob: {} (exists: {})", uploadedBlob.getName(), uploadedBlob.exists());
            return fileName;
        } catch (IOException e) {
            log.error("Failed to upload image: {}", fileName, e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    @Override
    public String getImageUrl(String fileName) {
        log.info("Getting image URL for fileName: {}, bucket: {}", fileName, bucketName);

        // In emulator mode, return direct URL without blob existence check
        // (fake-gcs-server has issues with storage.get())
        if (useEmulator) {
            return "http://" + emulatorHost + "/storage/v1/b/" + bucketName + "/o/" +
                    fileName.replace("/", "%2F") + "?alt=media";
        }

        // In production, verify blob exists before generating signed URL
        BlobId blobId = BlobId.of(bucketName, fileName);
        Blob blob = storage.get(blobId);

        if (blob == null) {
            log.error("Blob not found for fileName: {}", fileName);
            return null;
        }

        log.info("Blob found: {} (exists: {})", blob.getName(), blob.exists());
        return generateSignedUrl(fileName, 15);
    }

    @Override
    public void deleteImage(String fileName) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        storage.delete(blobId);
    }

    @Override
    public String generateSignedUrl(String fileName, long expirationTimeInMinutes) {
        try {
            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            // For Cloud Run, use IAM-based signing with ImpersonatedCredentials
            // This requires the service account to have roles/iam.serviceAccountTokenCreator on itself
            if (!useEmulator && serviceAccountEmail.isPresent() && !serviceAccountEmail.get().isEmpty()) {
                String saEmail = serviceAccountEmail.get();
                log.info("Using IAM-based signing with service account: {}", saEmail);
                
                // Get the current credentials and create an impersonated version
                GoogleCredentials sourceCredentials = GoogleCredentials.getApplicationDefault();
                ImpersonatedCredentials impersonatedCredentials = ImpersonatedCredentials.create(
                        sourceCredentials,
                        saEmail,
                        null,  // delegates
                        List.of("https://www.googleapis.com/auth/devstorage.read_write"),
                        300  // lifetime in seconds
                );
                
                // Create a new Storage client with impersonated credentials for signing
                Storage signingStorage = StorageOptions.newBuilder()
                        .setCredentials(impersonatedCredentials)
                        .setProjectId(projectId)
                        .build()
                        .getService();
                
                URL signedUrl = signingStorage.signUrl(
                        blobInfo,
                        expirationTimeInMinutes,
                        TimeUnit.MINUTES,
                        Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                        Storage.SignUrlOption.withV4Signature()
                );
                return signedUrl.toString();
            }

            // Fallback to standard signing (works locally with service account key file)
            log.info("Using standard credential-based signing");
            URL signedUrl = storage.signUrl(
                    blobInfo,
                    expirationTimeInMinutes,
                    TimeUnit.MINUTES,
                    Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                    Storage.SignUrlOption.withV4Signature()
            );
            return signedUrl.toString();
        } catch (Exception e) {
            log.error("Failed to generate signed URL for: {}", fileName, e);
            throw new RuntimeException("Failed to generate signed URL", e);
        }
    }
}
