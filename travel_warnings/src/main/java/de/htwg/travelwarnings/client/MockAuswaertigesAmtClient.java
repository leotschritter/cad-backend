package de.htwg.travelwarnings.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * Mock implementation of AuswaertigesAmtClient for development/testing.
 *
 * Enable by setting in application.properties:
 * app.mock-external-api=true
 *
 * The real Auswärtiges Amt API has bot protection (Enodia) that blocks
 * automated requests, making this mock necessary for development.
 */
@ApplicationScoped
public class MockAuswaertigesAmtClient implements AuswaertigesAmtClient {

    private static final Logger LOG = Logger.getLogger(MockAuswaertigesAmtClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final boolean mockEnabled;

    public MockAuswaertigesAmtClient(
            @ConfigProperty(name = "app.mock-external-api", defaultValue = "false") boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
        if (mockEnabled) {
            LOG.warn("=".repeat(80));
            LOG.warn("MOCK MODE ENABLED - Using mock travel warning data");
            LOG.warn("Real Auswärtiges Amt API is blocked by bot protection (Enodia)");
            LOG.warn("Set app.mock-external-api=false to attempt real API calls");
            LOG.warn("=".repeat(80));
        }
    }

    @Override
    public JsonNode getTravelWarnings() {
        if (!mockEnabled) {
            throw new UnsupportedOperationException("Mock is disabled");
        }

        LOG.info("Returning mock travel warnings data");

        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode responseData = objectMapper.createObjectNode();

        // Create content list
        ArrayNode contentList = objectMapper.createArrayNode();
        contentList.add("199124"); // Afghanistan
        contentList.add("199128"); // Albania
        contentList.add("220434"); // Germany
        contentList.add("208190"); // Ukraine
        contentList.add("199148"); // France

        responseData.set("contentList", contentList);

        // Add mock warning data for each country
        long now = Instant.now().toEpochMilli();

        // Afghanistan - High risk
        responseData.set("199124", createMockWarning(
            now, "Afghanistan: Reise- und Sicherheitshinweise",
            "AF", "Afghanistan", true, false, true, false
        ));

        // Albania - Low risk
        responseData.set("199128", createMockWarning(
            now, "Albanien: Reise- und Sicherheitshinweise",
            "AL", "Albania", false, false, false, false
        ));

        // Germany - No warnings
        responseData.set("220434", createMockWarning(
            now, "Deutschland: Reise- und Sicherheitshinweise",
            "DE", "Germany", false, false, false, false
        ));

        // Ukraine - Very high risk
        responseData.set("208190", createMockWarning(
            now, "Ukraine: Reise- und Sicherheitshinweise",
            "UA", "Ukraine", true, true, true, true
        ));

        // France - Low risk
        responseData.set("199148", createMockWarning(
            now, "Frankreich: Reise- und Sicherheitshinweise",
            "FR", "France", false, false, false, false
        ));

        response.set("response", responseData);

        return response;
    }

    @Override
    public JsonNode getTravelWarningDetail(@PathParam("contentId") String contentId) {
        if (!mockEnabled) {
            throw new UnsupportedOperationException("Mock is disabled");
        }

        LOG.infof("Returning mock travel warning detail for contentId: %s", contentId);

        ObjectNode response = objectMapper.createObjectNode();
        ObjectNode responseData = objectMapper.createObjectNode();

        long now = Instant.now().toEpochMilli();

        // Return detailed mock data based on contentId
        ObjectNode warningDetail;
        switch (contentId) {
            case "199124": // Afghanistan
                warningDetail = createDetailedMockWarning(
                    now, "Afghanistan: Reise- und Sicherheitshinweise",
                    "AF", "AFG", "Afghanistan", true, false, true, false,
                    "<h2>Sicherheit</h2><p>Vor Reisen nach Afghanistan wird gewarnt. " +
                    "Es besteht landesweit ein hohes Risiko für Anschläge und Entführungen.</p>"
                );
                break;
            case "199128": // Albania
                warningDetail = createDetailedMockWarning(
                    now, "Albanien: Reise- und Sicherheitshinweise",
                    "AL", "ALB", "Albania", false, false, false, false,
                    "<h2>Sicherheit</h2><p>Albanien ist grundsätzlich sicher zu bereisen. " +
                    "Übliche Vorsichtsmaßnahmen sollten beachtet werden.</p>"
                );
                break;
            case "220434": // Germany
                warningDetail = createDetailedMockWarning(
                    now, "Deutschland: Reise- und Sicherheitshinweise",
                    "DE", "DEU", "Germany", false, false, false, false,
                    "<h2>Sicherheit</h2><p>Deutschland verfügt über eine gute Sicherheitslage.</p>"
                );
                break;
            case "208190": // Ukraine
                warningDetail = createDetailedMockWarning(
                    now, "Ukraine: Reise- und Sicherheitshinweise",
                    "UA", "UKR", "Ukraine", true, true, true, true,
                    "<h2>Sicherheit</h2><p>Vor Reisen in die Ukraine wird dringend gewarnt. " +
                    "Es herrscht Krieg. Lebensgefahr im gesamten Land.</p>"
                );
                break;
            case "199148": // France
                warningDetail = createDetailedMockWarning(
                    now, "Frankreich: Reise- und Sicherheitshinweise",
                    "FR", "FRA", "France", false, false, false, false,
                    "<h2>Sicherheit</h2><p>Frankreich ist grundsätzlich sicher zu bereisen. " +
                    "In Großstädten sollte auf Taschendiebe geachtet werden.</p>"
                );
                break;
            default:
                // Return generic mock for unknown IDs
                warningDetail = createDetailedMockWarning(
                    now, "Mock Country: Reise- und Sicherheitshinweise",
                    "XX", "XXX", "Mock Country", false, false, false, false,
                    "<h2>Sicherheit</h2><p>Mock travel warning content.</p>"
                );
        }

        responseData.set(contentId, warningDetail);
        response.set("response", responseData);

        return response;
    }

    private ObjectNode createMockWarning(long timestamp, String title,
                                        String countryCode, String countryName,
                                        boolean warning, boolean partialWarning,
                                        boolean situationWarning, boolean situationPartWarning) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("lastModified", timestamp);
        node.put("effective", timestamp);
        node.put("title", title);
        node.put("countryCode", countryCode);
        node.put("countryName", countryName);
        node.put("warning", warning);
        node.put("partialWarning", partialWarning);
        node.put("situationWarning", situationWarning);
        node.put("situationPartWarning", situationPartWarning);
        return node;
    }

    private ObjectNode createDetailedMockWarning(long timestamp, String title,
                                                 String countryCode, String iso3CountryCode, String countryName,
                                                 boolean warning, boolean partialWarning,
                                                 boolean situationWarning, boolean situationPartWarning,
                                                 String content) {
        ObjectNode node = createMockWarning(timestamp, title, countryCode, countryName,
                                           warning, partialWarning, situationWarning, situationPartWarning);
        node.put("iso3CountryCode", iso3CountryCode);
        node.put("content", content);
        return node;
    }
}

