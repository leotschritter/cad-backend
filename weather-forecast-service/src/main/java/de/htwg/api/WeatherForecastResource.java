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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Optional;

@Path("/api/weather")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Weather Forecast", description = "Operations for managing weather forecasts")
public class WeatherForecastResource {

    @Inject
    WeatherForecastService weatherForecastService;

    @POST
    @Path("/forecast/coordinates")
    @Operation(
            summary = "Fetch Weather Forecast By Coordinates",
            description = "Fetches weather forecast data from external API and stores it in the database. Returns the forecast for the specified coordinates and location."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Weather forecast fetched and stored successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = WeatherForecastResponse.class),
                            examples = @ExampleObject(
                                    name = "Weather Forecast Example",
                                    summary = "Example of a weather forecast response",
                                    value = """
                                    {
                                      "id": 1,
                                      "location": "Berlin",
                                      "latitude": 52.52,
                                      "longitude": 13.405,
                                      "timezone": "UTC",
                                      "lastUpdated": "2025-11-22T10:30:00",
                                      "dailyForecasts": [
                                        {
                                          "date": "2025-11-22",
                                          "temperatureHigh": 15.5,
                                          "temperatureLow": 8.2,
                                          "weather": "Partly cloudy",
                                          "weatherIcon": "partly_cloudy",
                                          "precipitationProbability": 20,
                                          "precipitationTotal": 0.0,
                                          "windSpeed": 12.5,
                                          "windDirection": 180,
                                          "humidity": 65,
                                          "uvIndex": 3,
                                          "summary": "Mild temperatures with occasional clouds"
                                        }
                                      ],
                                      "hourlyForecasts": []
                                    }
                                    """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Bad request - Missing required parameters",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"Latitude, longitude and location are required\"}"
                    )
            )
    })
    public Response fetchWeatherForecastByCoordinates(
            @Parameter(description = "Latitude coordinate") @QueryParam("lat") Double latitude,
            @Parameter(description = "Longitude coordinate") @QueryParam("lon") Double longitude,
            @Parameter(description = "Location name") @QueryParam("location") String location) {

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
    @Path("/forecast/location")
    @Operation(
            summary = "Get Weather Forecast By Location",
            description = "Retrieves stored weather forecasts for a specific location. Returns all forecasts matching the location name (case-insensitive)."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Weather forecasts retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = WeatherForecastResponse[].class),
                            examples = @ExampleObject(
                                    name = "Forecast List Example",
                                    summary = "Example of weather forecast list for a location",
                                    value = """
                                    [
                                      {
                                        "id": 1,
                                        "location": "Berlin",
                                        "latitude": 52.52,
                                        "longitude": 13.405,
                                        "timezone": "UTC",
                                        "lastUpdated": "2025-11-22T10:30:00",
                                        "dailyForecasts": [
                                          {
                                            "date": "2025-11-22",
                                            "temperatureHigh": 15.5,
                                            "temperatureLow": 8.2,
                                            "weather": "Partly cloudy",
                                            "weatherIcon": "partly_cloudy",
                                            "precipitationProbability": 20,
                                            "precipitationTotal": 0.0,
                                            "windSpeed": 12.5,
                                            "windDirection": 180,
                                            "humidity": 65,
                                            "uvIndex": 3,
                                            "summary": "Mild temperatures with occasional clouds"
                                          }
                                        ],
                                        "hourlyForecasts": []
                                      }
                                    ]
                                    """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "No weather forecast found for the specified location",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"Weather forecast not found for location: Berlin\"}"
                    )
            )
    })
    public Response getWeatherForecastByLocation(
            @Parameter(description = "Location name") @QueryParam("location") String location) {
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
    @Operation(
            summary = "Get Weather Forecast By Coordinates",
            description = "Retrieves stored weather forecasts for specific geographic coordinates. Returns all forecasts matching the latitude and longitude."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Weather forecasts retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = WeatherForecastResponse[].class),
                            examples = @ExampleObject(
                                    name = "Forecast List by Coordinates Example",
                                    summary = "Example of weather forecast list for coordinates",
                                    value = """
                                    [
                                      {
                                        "id": 1,
                                        "location": "Berlin",
                                        "latitude": 52.52,
                                        "longitude": 13.405,
                                        "timezone": "UTC",
                                        "lastUpdated": "2025-11-22T10:30:00",
                                        "dailyForecasts": [
                                          {
                                            "date": "2025-11-22",
                                            "temperatureHigh": 15.5,
                                            "temperatureLow": 8.2,
                                            "weather": "Partly cloudy",
                                            "weatherIcon": "partly_cloudy",
                                            "precipitationProbability": 20,
                                            "precipitationTotal": 0.0,
                                            "windSpeed": 12.5,
                                            "windDirection": 180,
                                            "humidity": 65,
                                            "uvIndex": 3,
                                            "summary": "Mild temperatures with occasional clouds"
                                          }
                                        ],
                                        "hourlyForecasts": []
                                      }
                                    ]
                                    """
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Bad request - Missing required parameters",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"Latitude and longitude are required\"}"
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "No weather forecast found for the specified coordinates",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"Weather forecast not found for coordinates: 52.52, 13.405\"}"
                    )
            )
    })
    public Response getWeatherForecastByCoordinates(
            @Parameter(description = "Latitude coordinate") @QueryParam("lat") Double latitude,
            @Parameter(description = "Longitude coordinate") @QueryParam("lon") Double longitude) {

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
    @Operation(
            summary = "Get All Weather Forecasts",
            description = "Retrieves all stored weather forecasts from the database. Returns a list of all available forecasts."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "All weather forecasts retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = WeatherForecastResponse[].class),
                            examples = @ExampleObject(
                                    name = "All Forecasts Example",
                                    summary = "Example of all weather forecasts",
                                    value = """
                                    [
                                      {
                                        "id": 1,
                                        "location": "Berlin",
                                        "latitude": 52.52,
                                        "longitude": 13.405,
                                        "timezone": "UTC",
                                        "lastUpdated": "2025-11-22T10:30:00",
                                        "dailyForecasts": [
                                          {
                                            "date": "2025-11-22",
                                            "temperatureHigh": 15.5,
                                            "temperatureLow": 8.2,
                                            "weather": "Partly cloudy",
                                            "weatherIcon": "partly_cloudy",
                                            "precipitationProbability": 20,
                                            "precipitationTotal": 0.0,
                                            "windSpeed": 12.5,
                                            "windDirection": 180,
                                            "humidity": 65,
                                            "uvIndex": 3,
                                            "summary": "Mild temperatures with occasional clouds"
                                          }
                                        ],
                                        "hourlyForecasts": []
                                      },
                                      {
                                        "id": 2,
                                        "location": "Munich",
                                        "latitude": 48.1351,
                                        "longitude": 11.582,
                                        "timezone": "UTC",
                                        "lastUpdated": "2025-11-22T10:35:00",
                                        "dailyForecasts": [
                                          {
                                            "date": "2025-11-22",
                                            "temperatureHigh": 18.0,
                                            "temperatureLow": 10.5,
                                            "weather": "Sunny",
                                            "weatherIcon": "sunny",
                                            "precipitationProbability": 5,
                                            "precipitationTotal": 0.0,
                                            "windSpeed": 8.0,
                                            "windDirection": 90,
                                            "humidity": 55,
                                            "uvIndex": 4,
                                            "summary": "Clear skies with warm temperatures"
                                          }
                                        ],
                                        "hourlyForecasts": []
                                      }
                                    ]
                                    """
                            )
                    )
            )
    })
    public Response getAllWeatherForecasts() {
        List<WeatherForecast> forecasts = weatherForecastService.getAllWeatherForecasts();

        List<WeatherForecastResponse> responses = forecasts.stream()
                .map(this::mapToResponse)
                .toList();

        return Response.ok(responses).build();
    }

    @DELETE
    @Path("/forecast/location")
    @Operation(
            summary = "Delete Weather Forecast By Location",
            description = "Deletes all weather forecasts for a specific location. Returns the number of deleted forecasts. Case-insensitive location matching."
    )
    @APIResponses(value = {
            @APIResponse(
                    responseCode = "200",
                    description = "Weather forecast(s) deleted successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            examples = @ExampleObject(
                                    name = "Delete Success Example",
                                    summary = "Example of successful deletion",
                                    value = "\"Successfully deleted 1 weather forecast(s) for location: Berlin\""
                            )
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Bad request - Missing location parameter",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"Location parameter is required\"}"
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "No weather forecast found for the specified location",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            example = "{\"error\": \"No weather forecast found for location: Berlin\"}"
                    )
            )
    })
    public Response deleteWeatherForecastByLocation(
            @Parameter(description = "Location name to delete forecasts for") @QueryParam("location") String location) {
        if (location == null || location.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Location parameter is required")
                    .build();
        }

        int deletedCount = weatherForecastService.deleteWeatherForecastByLocation(location);

        if (deletedCount == 0) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("No weather forecast found for location: " + location)
                    .build();
        }

        return Response.ok()
                .entity("Successfully deleted " + deletedCount + " weather forecast(s) for location: " + location)
                .build();
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
