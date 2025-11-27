package de.htwg.travelwarnings.api.resource;

import de.htwg.travelwarnings.persistence.entity.TravelWarning;
import de.htwg.travelwarnings.persistence.repository.TravelWarningRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class TravelWarningResourceTest {

    @Inject
    TravelWarningRepository warningRepository;

    @BeforeEach
    @Transactional
    void setup() {
        warningRepository.deleteAll();

        // Create test warning
        TravelWarning warning = new TravelWarning();
        warning.setContentId("TEST001");
        warning.setLastModified(System.currentTimeMillis());
        warning.setEffective(System.currentTimeMillis());
        warning.setTitle("Test Country: Travel Warning");
        warning.setCountryCode("DE");
        warning.setIso3CountryCode("DEU");
        warning.setCountryName("Germany");
        warning.setWarning(true);
        warning.setPartialWarning(false);
        warning.setSituationWarning(false);
        warning.setSituationPartWarning(false);
        warning.setContent("<h3>Test Content</h3>");
        warning.setFetchedAt(Instant.now());

        warningRepository.persist(warning);
    }

    @Test
    void testGetAllWarnings() {
        given()
            .when().get("/warnings/travel-warnings")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", is(1));
    }

    @Test
    void testGetWarningByCountryCode() {
        given()
            .when().get("/warnings/travel-warnings/country/DE")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("countryCode", is("DE"))
            .body("countryName", is("Germany"))
            .body("severity", notNullValue())
            .body("hasActiveWarning", is(true));
    }

    @Test
    void testGetWarningByCountryCodeNotFound() {
        given()
            .when().get("/warnings/travel-warnings/country/XX")
            .then()
            .statusCode(404);
    }

    @Test
    void testGetWarningDetail() {
        given()
            .when().get("/warnings/travel-warnings/country/DE/detail")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("warning.countryCode", is("DE"))
            .body("content", notNullValue())
            .body("officialLink", notNullValue());
    }

    @Test
    void testGetActiveWarningsOnly() {
        given()
            .queryParam("activeOnly", true)
            .when().get("/warnings/travel-warnings")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", is(1));
    }
}

