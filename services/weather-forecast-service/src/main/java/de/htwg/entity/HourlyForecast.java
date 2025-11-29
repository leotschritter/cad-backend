package de.htwg.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "hourly_forecasts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HourlyForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weather_forecast_id", nullable = false)
    private WeatherForecast weatherForecast;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private Double temperature;

    @Column(nullable = false)
    private String weather;

    @Column(name = "weather_icon")
    private String weatherIcon;

    @Column(name = "precipitation_total")
    private Double precipitationTotal;

    @Column(name = "precipitation_type")
    private String precipitationType;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "wind_direction")
    private Integer windDirection;

    @Column(name = "cloud_cover")
    private Integer cloudCover;

    @Column
    private String summary;
}
