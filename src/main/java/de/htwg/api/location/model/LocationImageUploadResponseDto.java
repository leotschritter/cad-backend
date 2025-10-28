package de.htwg.api.location.model;

import lombok.Builder;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Builder
@Schema(description = "Response object for location image upload operations")
public record LocationImageUploadResponseDto(
    @Schema(description = "Success message")
    String message,
    
    @Schema(description = "List of uploaded image URLs")
    List<String> imageUrls
) {
}

