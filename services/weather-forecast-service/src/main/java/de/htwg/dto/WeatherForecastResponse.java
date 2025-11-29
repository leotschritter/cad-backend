package de.htwg.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherForecastResponse {

    private Long id;
    private String location;
    private Double latitude;
    private Double longitude;
    private String timezone;
    private LocalDateTime lastUpdated;
    private List<DailyForecastResponse> dailyForecasts;
    private List<HourlyForecastResponse> hourlyForecasts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyForecastResponse {
        private String date;
        private Double temperatureHigh;
        private Double temperatureLow;
        private String weather;
        private String weatherIcon;
        private Integer precipitationProbability;
        private Double precipitationTotal;
        private Double windSpeed;
        private Integer windDirection;
        private Integer humidity;
        private Integer uvIndex;
        private String summary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HourlyForecastResponse {
        private String date;
        private Double temperature;
        private String weather;
        private String weatherIcon;
        private Double precipitationTotal;
        private String precipitationType;
        private Double windSpeed;
        private Integer windDirection;
        private Integer cloudCover;
        private String summary;
    }
}
