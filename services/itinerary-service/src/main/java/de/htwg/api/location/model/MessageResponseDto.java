package de.htwg.api.location.model;

import lombok.Builder;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Builder
@Schema(description = "Response object for successful operations")
public record MessageResponseDto(
    @Schema(description = "Success or info message")
    String message
) {
}

