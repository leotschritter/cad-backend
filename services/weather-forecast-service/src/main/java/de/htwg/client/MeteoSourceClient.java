package de.htwg.client;

import de.htwg.dto.MeteoSourceResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "meteosource-api")
public interface MeteoSourceClient {

    @GET
    @Path("/point")
    MeteoSourceResponse getWeatherForecast(
            @QueryParam("lat") Double latitude,
            @QueryParam("lon") Double longitude,
            @QueryParam("sections") String sections,
            @QueryParam("timezone") String timezone,
            @QueryParam("language") String language,
            @QueryParam("units") String units,
            @QueryParam("key") String apiKey
    );
}
