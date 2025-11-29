package de.htwg.service.storage;

import java.io.InputStream;

public interface ImageStorageService {
    
    String uploadImage(InputStream imageStream, String fileName, String contentType);
    
    String getImageUrl(String fileName);
    
    void deleteImage(String fileName);
    
    String generateSignedUrl(String fileName, long expirationTimeInMinutes);
}
