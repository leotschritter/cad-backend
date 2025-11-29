package de.htwg.travelwarnings.client;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


/**
 * REST Client for Auswärtiges Amt OpenData API.
 * Fetches official travel warnings and safety information.
 */
@RegisterRestClient(configKey = "de.htwg.travelwarnings.client.AuswaertigesAmtClient")
public interface AuswaertigesAmtClient {

    /**
     * Get all travel warnings (summary view without content)
     */
    @GET
    @Path("travelwarning")
    @Produces(MediaType.APPLICATION_JSON)
    JsonNode getTravelWarnings();

    /**
     * Get detailed travel warning by content ID (includes full content)
     */
    @GET
    @Path("travelwarning/{contentId}")
    @Produces(MediaType.APPLICATION_JSON)
    JsonNode getTravelWarningDetail(@PathParam("contentId") String contentId);
}

