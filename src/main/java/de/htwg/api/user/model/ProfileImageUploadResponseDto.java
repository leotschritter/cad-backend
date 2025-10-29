package de.htwg.api.user.model;

import lombok.Builder;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Builder
@Schema(description = "Response object for profile image upload operations")
public record







ProfileImageUploadResponseDto(
    @Schema(description = "Success message")
    String message,

    @Schema(description = "URL of the uploaded image")
    String imageUrl
) {
}

