package de.htwg.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "daily_forecasts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weather_forecast_id", nullable = false)
    private WeatherForecast weatherForecast;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "temperature_high", nullable = false)
    private Double temperatureHigh;

    @Column(name = "temperature_low", nullable = false)
    private Double temperatureLow;

    @Column(nullable = false)
    private String weather;

    @Column(name = "weather_icon")
    private String weatherIcon;

    @Column(name = "precipitation_probability")
    private Integer precipitationProbability;

    @Column(name = "precipitation_total")
    private Double precipitationTotal;

    @Column(name = "wind_speed")
    private Double windSpeed;

    @Column(name = "wind_direction")
    private Integer windDirection;

    @Column
    private Integer humidity;

    @Column(name = "uv_index")
    private Integer uvIndex;

    @Column
    private String summary;
}
