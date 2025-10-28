package de.htwg.api.user.model;

import lombok.Builder;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Builder
@Schema(description = "Response object containing the profile image URL")
public record ProfileImageResponseDto(
    @Schema(description = "URL of the profile image")
    String imageUrl
) {
}

