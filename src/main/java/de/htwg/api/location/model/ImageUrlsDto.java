package de.htwg.api.location.model;

import lombok.Builder;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Builder
public record ImageUrlsDto(
    @Schema(
        description = "List of image URLs (filenames) to add to the location",
        example = "[\"location-images/1/image1.jpg\", \"location-images/1/image2.jpg\"]",
        required = true
    )
    List<String> imageUrls
) {
}
