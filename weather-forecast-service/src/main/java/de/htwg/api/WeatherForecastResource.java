package de.htwg.api;

import de.htwg.entity.DailyForecast;
import de.htwg.entity.HourlyForecast;
import de.htwg.entity.WeatherForecast;
import de.htwg.dto.WeatherForecastResponse;
import de.htwg.service.WeatherForecastService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

@Path("/api/weather")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WeatherForecastResource {

    @Inject
    WeatherForecastService weatherForecastService;

    @POST
    @Path("/forecast/coordinates")
    public Response fetchWeatherForecastByCoordinates(
            @QueryParam("lat") Double latitude,
            @QueryParam("lon") Double longitude,
            @QueryParam("location") String location) {

        if (latitude == null || longitude == null || location == null || location.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Latitude, longitude and location are required")
                    .build();
        }

        WeatherForecast forecast = weatherForecastService.fetchAndStoreWeatherForecast(
                latitude, longitude, location);

        return Response.ok(mapToResponse(forecast)).build();
    }

    @GET
    @Path("/forecast/location/{location}")
    public Response getWeatherForecastByLocation(@PathParam("location") String location) {
        List<WeatherForecast> forecast = weatherForecastService.getWeatherForecastByLocation(location);

        if (forecast.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Weather forecast not found for location: " + location)
                    .build();
        }

        List<WeatherForecastResponse> response = forecast.stream()
                .map(this::mapToResponse)
                .toList();

        return Response.ok(response).build();
    }

    @GET
    @Path("/forecast/coordinates")
    public Response getWeatherForecastByCoordinates(
            @QueryParam("lat") Double latitude,
            @QueryParam("lon") Double longitude) {

        if (latitude == null || longitude == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Latitude and longitude are required")
                    .build();
        }

        List<WeatherForecast> forecast = weatherForecastService
                .getWeatherForecastByCoordinates(latitude, longitude);

        if (forecast.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Weather forecast not found for coordinates: " + latitude + ", " + longitude)
                    .build();
        }

        List<WeatherForecastResponse> response = forecast.stream()
                .map(this::mapToResponse)
                .toList();

        return Response.ok(response).build();
    }

    @GET
    @Path("/forecasts")
    public Response getAllWeatherForecasts() {
        List<WeatherForecast> forecasts = weatherForecastService.getAllWeatherForecasts();

        List<WeatherForecastResponse> responses = forecasts.stream()
                .map(this::mapToResponse)
                .toList();

        return Response.ok(responses).build();
    }

    private WeatherForecastResponse mapToResponse(WeatherForecast forecast) {
        List<WeatherForecastResponse.DailyForecastResponse> dailyResponses = forecast.getDailyForecasts()
                .stream()
                .map(this::mapToDailyResponse)
                .toList();

        List<WeatherForecastResponse.HourlyForecastResponse> hourlyResponses = forecast.getHourlyForecasts()
                .stream()
                .map(this::mapToHourlyResponse)
                .toList();

        return WeatherForecastResponse.builder()
                .id(forecast.getId())
                .location(forecast.getLocation())
                .latitude(forecast.getLatitude())
                .longitude(forecast.getLongitude())
                .timezone(forecast.getTimezone())
                .lastUpdated(forecast.getLastUpdated())
                .dailyForecasts(dailyResponses)
                .hourlyForecasts(hourlyResponses)
                .build();
    }

    private WeatherForecastResponse.DailyForecastResponse mapToDailyResponse(DailyForecast daily) {
        return WeatherForecastResponse.DailyForecastResponse.builder()
                .date(daily.getDate().toString())
                .temperatureHigh(daily.getTemperatureHigh())
                .temperatureLow(daily.getTemperatureLow())
                .weather(daily.getWeather())
                .weatherIcon(daily.getWeatherIcon())
                .precipitationProbability(daily.getPrecipitationProbability())
                .precipitationTotal(daily.getPrecipitationTotal())
                .windSpeed(daily.getWindSpeed())
                .windDirection(daily.getWindDirection())
                .humidity(daily.getHumidity())
                .uvIndex(daily.getUvIndex())
                .summary(daily.getSummary())
                .build();
    }

    private WeatherForecastResponse.HourlyForecastResponse mapToHourlyResponse(HourlyForecast hourly) {
        return WeatherForecastResponse.HourlyForecastResponse.builder()
                .date(hourly.getDate().toString())
                .temperature(hourly.getTemperature())
                .weather(hourly.getWeather())
                .weatherIcon(hourly.getWeatherIcon())
                .precipitationTotal(hourly.getPrecipitationTotal())
                .precipitationType(hourly.getPrecipitationType())
                .windSpeed(hourly.getWindSpeed())
                .windDirection(hourly.getWindDirection())
                .cloudCover(hourly.getCloudCover())
                .summary(hourly.getSummary())
                .build();
    }
}
