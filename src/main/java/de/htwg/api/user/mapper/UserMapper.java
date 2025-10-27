package de.htwg.api.user.mapper;

import de.htwg.api.user.model.UserDto;
import de.htwg.persistence.entity.User;
import de.htwg.service.storage.ImageStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserMapper {

    private final ImageStorageService imageStorageService;

    @Inject
    public UserMapper(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    public User toEntity(UserDto dto) {
        return User.builder()
                .name(dto.name())
                .email(dto.email())
                .profileImageUrl(dto.profileImageUrl())
                .build();
    }

    public UserDto toDto(User entity) {
        // Generate fresh signed URL for profile image
        String signedProfileImageUrl = null;
        if (entity.getProfileImageUrl() != null) {
            String fileName = extractFileNameFromUrl(entity.getProfileImageUrl());
            signedProfileImageUrl = imageStorageService.getImageUrl(fileName);
        }

        return UserDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .profileImageUrl(signedProfileImageUrl)
                .build();
    }
    
    /**
     * Extract filename from a signed URL or raw GCS URL
     */
    private String extractFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // Remove query parameters (signed URL signature)
        String urlWithoutQuery = url.split("\\?")[0];
        
        // Extract filename from path
        String[] pathParts = urlWithoutQuery.split("/");
        if (pathParts.length > 0) {
            return pathParts[pathParts.length - 1];
        }
        
        return url; // Fallback to original URL
    }
}
