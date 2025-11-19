package de.htwg.service;

import de.htwg.client.MeteoSourceClient;
import de.htwg.entity.DailyForecast;
import de.htwg.entity.HourlyForecast;
import de.htwg.entity.WeatherForecast;
import de.htwg.dto.MeteoSourceResponse;
import de.htwg.persistence.WeatherForecastRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class WeatherForecastService {

    @Inject
    @RestClient
    MeteoSourceClient meteoSourceClient;

    @Inject
    WeatherForecastRepository weatherForecastRepository;

    @ConfigProperty(name = "meteosource.api.key")
    String apiKey;

    @ConfigProperty(name = "meteosource.api.sections", defaultValue = "daily")
    String sections;

    @ConfigProperty(name = "meteosource.api.language", defaultValue = "en")
    String language;

    @ConfigProperty(name = "meteosource.api.units", defaultValue = "metric")
    String units;

    @Transactional
    public WeatherForecast fetchAndStoreWeatherForecast(Double latitude, Double longitude, String location) {
        // Fetch from Meteosource API
        MeteoSourceResponse response = meteoSourceClient.getWeatherForecast(
                latitude,
                longitude,
                sections,
                "UTC",
                language,
                units,
                apiKey
        );

        // Check if forecast already exists
        List<WeatherForecast> existingForecast = weatherForecastRepository
                .findByCoordinates(latitude, longitude);

        WeatherForecast weatherForecast;
        if (!existingForecast.isEmpty()) {
            weatherForecast = existingForecast.getFirst();
            weatherForecast.setLocation(location);
            weatherForecast.setLastUpdated(LocalDateTime.now());
            weatherForecast.getDailyForecasts().clear();
            weatherForecast.getHourlyForecasts().clear();
        } else {
            weatherForecast = WeatherForecast.builder()
                    .location(location)
                    .latitude(latitude)
                    .longitude(longitude)
                    .timezone(response.getTimezone() != null ? response.getTimezone() : "UTC")
                    .dailyForecasts(new ArrayList<>())
                    .build();
        }

        // Map daily forecasts
        if (response.getDaily() != null && response.getDaily().getData() != null) {
            List<DailyForecast> dailyForecasts = response.getDaily().getData().stream()
                    .map(data -> mapToDailyForecast(data, weatherForecast))
                    .toList();
            weatherForecast.getDailyForecasts().addAll(dailyForecasts);
        }

        // Map hourly forecasts
        if (response.getHourly() != null && response.getHourly().getData() != null) {
            List<HourlyForecast> hourlyForecasts = response.getHourly().getData().stream()
                    .map(data -> mapToHourlyForecast(data, weatherForecast))
                    .toList();
            weatherForecast.getHourlyForecasts().addAll(hourlyForecasts);
        }

        // Save to database
        weatherForecastRepository.persist(weatherForecast);

        return weatherForecast;
    }

    public List<WeatherForecast> getWeatherForecastByLocation(String location) {
        return weatherForecastRepository.findByLocation(location);
    }

    public List<WeatherForecast> getWeatherForecastByCoordinates(Double latitude, Double longitude) {
        return weatherForecastRepository.findByCoordinates(latitude, longitude);
    }

    public List<WeatherForecast> getAllWeatherForecasts() {
        return weatherForecastRepository.listAll();
    }

    private DailyForecast mapToDailyForecast(
            MeteoSourceResponse.DailyForecastData data,
            WeatherForecast weatherForecast) {

        MeteoSourceResponse.DailyForecastData.AllDayData allDay = data.getAllDay();

        return DailyForecast.builder()
                .weatherForecast(weatherForecast)
                .date(LocalDate.parse(data.getDay()))
                .weather(data.getWeather() != null ? data.getWeather() : "Unknown")
                .weatherIcon(data.getIcon())
                .summary(data.getSummary())
                .temperatureHigh(allDay != null ? allDay.getTemperatureMax() : null)
                .temperatureLow(allDay != null ? allDay.getTemperatureMin() : null)
                .precipitationProbability(allDay != null && allDay.getPrecipitation() != null ?
                        allDay.getPrecipitation().getProbability() : null)
                .precipitationTotal(allDay != null && allDay.getPrecipitation() != null ?
                        allDay.getPrecipitation().getTotal() : null)
                .windSpeed(allDay != null && allDay.getWind() != null ?
                        allDay.getWind().getSpeed() : null)
                .windDirection(allDay != null && allDay.getWind() != null ?
                        allDay.getWind().getAngle() : null)
                .humidity(allDay != null ? allDay.getHumidity() : null)
                .uvIndex(allDay != null ? allDay.getUvIndex() : null)
                .build();
    }

    private HourlyForecast mapToHourlyForecast(
            MeteoSourceResponse.HourlyForecastData data,
            WeatherForecast weatherForecast) {

        return HourlyForecast.builder()
                .weatherForecast(weatherForecast)
                .date(LocalDateTime.parse(data.getDate()))
                .weather(data.getWeather() != null ? data.getWeather() : "Unknown")
                .weatherIcon(data.getIcon())
                .summary(data.getSummary())
                .temperature(data.getTemperature())
                .precipitationTotal(data.getPrecipitation() != null ?
                        data.getPrecipitation().getTotal() : null)
                .precipitationType(data.getPrecipitation() != null ?
                        data.getPrecipitation().getType() : null)
                .windSpeed(data.getWind() != null ?
                        data.getWind().getSpeed() : null)
                .windDirection(data.getWind() != null ?
                        data.getWind().getAngle() : null)
                .cloudCover(data.getCloudCover() != null ?
                        data.getCloudCover().getTotal() : null)
                .build();
    }
}
