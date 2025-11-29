package de.htwg.dto.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class CoordinateWrapper {
        @JsonDeserialize(using = CoordinateDeserializer.class)
        private Double coordinate;
    }

    @Test
    void testDeserializeNorthCoordinate() throws Exception {
        String json = "{\"coordinate\":\"51.50853N\"}";
        CoordinateWrapper result = objectMapper.readValue(json, CoordinateWrapper.class);

        assertEquals(51.50853, result.getCoordinate(), 0.00001);
    }

    @Test
    void testDeserializeSouthCoordinate() throws Exception {
        String json = "{\"coordinate\":\"33.86895S\"}";
        CoordinateWrapper result = objectMapper.readValue(json, CoordinateWrapper.class);

        assertEquals(-33.86895, result.getCoordinate(), 0.00001);
    }

    @Test
    void testDeserializeEastCoordinate() throws Exception {
        String json = "{\"coordinate\":\"151.20732E\"}";
        CoordinateWrapper result = objectMapper.readValue(json, CoordinateWrapper.class);

        assertEquals(151.20732, result.getCoordinate(), 0.00001);
    }

    @Test
    void testDeserializeWestCoordinate() throws Exception {
        String json = "{\"coordinate\":\"0.12574W\"}";
        CoordinateWrapper result = objectMapper.readValue(json, CoordinateWrapper.class);

        assertEquals(-0.12574, result.getCoordinate(), 0.00001);
    }

    @Test
    void testDeserializeNumericValue() throws Exception {
        String json = "{\"coordinate\":48.8566}";
        CoordinateWrapper result = objectMapper.readValue(json, CoordinateWrapper.class);

        assertEquals(48.8566, result.getCoordinate(), 0.00001);
    }

    @Test
    void testDeserializeNegativeNumericValue() throws Exception {
        String json = "{\"coordinate\":-73.9352}";
        CoordinateWrapper result = objectMapper.readValue(json, CoordinateWrapper.class);

        assertEquals(-73.9352, result.getCoordinate(), 0.00001);
    }

    @Test
    void testDeserializeNull() throws Exception {
        String json = "{\"coordinate\":null}";
        CoordinateWrapper result = objectMapper.readValue(json, CoordinateWrapper.class);

        assertNull(result.getCoordinate());
    }

    @Test
    void testDeserializeEmptyString() throws Exception {
        String json = "{\"coordinate\":\"\"}";
        CoordinateWrapper result = objectMapper.readValue(json, CoordinateWrapper.class);

        assertNull(result.getCoordinate());
    }

    @Test
    void testDeserializeInvalidDirection() {
        String json = "{\"coordinate\":\"51.50853X\"}";

        assertThrows(Exception.class, () ->
            objectMapper.readValue(json, CoordinateWrapper.class)
        );
    }

    @Test
    void testRealWorldExamples() throws Exception {
        // London
        String londonJson = "{\"coordinate\":\"51.50853N\"}";
        CoordinateWrapper london = objectMapper.readValue(londonJson, CoordinateWrapper.class);
        assertEquals(51.50853, london.getCoordinate(), 0.00001);

        // Sydney (South)
        String sydneyJson = "{\"coordinate\":\"33.86895S\"}";
        CoordinateWrapper sydney = objectMapper.readValue(sydneyJson, CoordinateWrapper.class);
        assertEquals(-33.86895, sydney.getCoordinate(), 0.00001);

        // New York (West)
        String nyJson = "{\"coordinate\":\"74.0060W\"}";
        CoordinateWrapper ny = objectMapper.readValue(nyJson, CoordinateWrapper.class);
        assertEquals(-74.0060, ny.getCoordinate(), 0.00001);
    }
}
