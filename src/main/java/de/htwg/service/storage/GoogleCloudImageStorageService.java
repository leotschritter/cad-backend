package de.htwg.service.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class GoogleCloudImageStorageService implements ImageStorageService {

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

    private Storage storage;

    @PostConstruct
    void init() throws IOException {
        StorageOptions.Builder storageOptionsBuilder = StorageOptions.newBuilder()
                .setProjectId(projectId);


        if (useEmulator) {
            // For fake-gcs-server emulator, we need to set the host properly
            // The emulator expects requests at http://host:port/storage/v1/
            storageOptionsBuilder
                    .setHost("http://" + emulatorHost)
                    .setCredentials(NoCredentials.getInstance());
        } else {
            storageOptionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
        }
        this.storage = storageOptionsBuilder.build().getService();

    }


    @Override
    public String uploadImage(InputStream imageStream, String fileName, String contentType) {
        try {
            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();

            Blob uploadedBlob = storage.create(blobInfo, imageStream.readAllBytes());
            System.out.println("Successfully uploaded blob: " + uploadedBlob.getName() +
                             " (exists: " + uploadedBlob.exists() + ")");
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    @Override
    public String getImageUrl(String fileName) {
        System.out.println("Getting image URL for fileName: " + fileName + ", bucket: " + bucketName);

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
            System.err.println("Blob not found for fileName: " + fileName);
            return null;
        }

        System.out.println("Blob found: " + blob.getName() + " (exists: " + blob.exists() + ")");
        return generateSignedUrl(fileName, 15);
    }

    @Override
    public void deleteImage(String fileName) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        storage.delete(blobId);
    }

    @Override
    public String generateSignedUrl(String fileName, long expirationTimeInMinutes) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        
        URL signedUrl = storage.signUrl(blobInfo, expirationTimeInMinutes, TimeUnit.MINUTES);
        return signedUrl.toString();
    }
}
