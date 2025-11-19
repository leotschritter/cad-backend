package de.htwg.dto.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Custom deserializer for geographic coordinates.
 * Handles both numeric values (e.g., 51.50853) and string values with direction (e.g., "51.50853N").
 */
public class CoordinateDeserializer extends JsonDeserializer<Double> {

    @Override
    public Double deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getText();

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        // Try to parse as double directly first
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // If that fails, try to parse as coordinate with direction (e.g., "51.50853N")
            return parseCoordinateWithDirection(value);
        }
    }

    /**
     * Parse coordinate string with direction suffix (N/S/E/W).
     * Examples: "51.50853N", "0.12574W", "48.8566S", "2.3522E"
     *
     * @param value The coordinate string with direction
     * @return The parsed coordinate as Double, with appropriate sign
     */
    private Double parseCoordinateWithDirection(String value) {
        value = value.trim();

        if (value.isEmpty()) {
            return null;
        }

        // Extract the direction (last character)
        char direction = value.charAt(value.length() - 1);
        String numericPart = value.substring(0, value.length() - 1).trim();

        // Parse the numeric part
        double coordinate;
        try {
            coordinate = Double.parseDouble(numericPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid coordinate format: " + value, e);
        }

        // Apply sign based on direction
        // North (N) and East (E) are positive
        // South (S) and West (W) are negative
        switch (Character.toUpperCase(direction)) {
            case 'N':
            case 'E':
                return coordinate;
            case 'S':
            case 'W':
                return -coordinate;
            default:
                throw new IllegalArgumentException(
                    "Invalid direction in coordinate: " + direction + " (expected N, S, E, or W)");
        }
    }
}
