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
        // Generate fresh signed URL (or direct URL in dev) for profile image
        // The entity.getProfileImageUrl() contains the filename path, not a URL
        String signedProfileImageUrl = null;
        if (entity.getProfileImageUrl() != null && !entity.getProfileImageUrl().isEmpty()) {
            signedProfileImageUrl = imageStorageService.getImageUrl(entity.getProfileImageUrl());
        }

        return UserDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .profileImageUrl(signedProfileImageUrl)
                .build();
    }
}
