package de.htwg.travelwarnings.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard error response DTO for API errors
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("status")
    private int status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    @JsonProperty("path")
    private String path;

    /**
     * Factory method for creating error responses
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }

    /**
     * Factory method for NOT_FOUND errors
     */
    public static ErrorResponse notFound(String message, String path) {
        return of(404, "Not Found", message, path);
    }

    /**
     * Factory method for BAD_REQUEST errors
     */
    public static ErrorResponse badRequest(String message, String path) {
        return of(400, "Bad Request", message, path);
    }
}

