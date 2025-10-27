package de.htwg.service.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
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

    private Storage storage;

    private Storage getStorage() {
        if (storage == null) {
            storage = StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .build()
                    .getService();
        }
        return storage;
    }

    @Override
    public String uploadImage(InputStream imageStream, String fileName, String contentType) {
        try {
            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();

            getStorage().create(blobInfo, imageStream.readAllBytes());
            // Return signed URL instead of raw GCS URL
            return generateSignedUrl(fileName, 15); // 15 minutes expiration
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    @Override
    public String getImageUrl(String fileName) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        Blob blob = getStorage().get(blobId);
        if (blob == null) {
            return null;
        }
        // Return signed URL instead of raw GCS URL
        return generateSignedUrl(fileName, 15); // 15 minutes expiration
    }

    @Override
    public void deleteImage(String fileName) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        getStorage().delete(blobId);
    }

    @Override
    public String generateSignedUrl(String fileName, long expirationTimeInMinutes) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        
        URL signedUrl = getStorage().signUrl(blobInfo, expirationTimeInMinutes, TimeUnit.MINUTES);
        return signedUrl.toString();
    }
}
