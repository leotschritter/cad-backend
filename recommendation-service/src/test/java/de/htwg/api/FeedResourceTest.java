package de.htwg.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class FeedResourceTest {

    @Test
    void testGetPersonalizedFeedWithoutTravellerId() {
        given()
            .when()
            .get("/api/v1/feed")
            .then()
            .statusCode(400);
    }

    @Test
    void testGetPersonalizedFeedWithValidTravellerId() {
        given()
            .queryParam("travellerId", "test-user-123")
            .when()
            .get("/api/v1/feed")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("items", notNullValue())
            .body("page", is(0))
            .body("pageSize", is(20));
    }

    @Test
    void testGetPersonalizedFeedWithPagination() {
        given()
            .queryParam("travellerId", "test-user-123")
            .queryParam("page", 1)
            .queryParam("pageSize", 10)
            .when()
            .get("/api/v1/feed")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("page", is(1))
            .body("pageSize", is(10));
    }

    @Test
    void testGetPersonalizedFeedWithInvalidPage() {
        given()
            .queryParam("travellerId", "test-user-123")
            .queryParam("page", -1)
            .when()
            .get("/api/v1/feed")
            .then()
            .statusCode(400);
    }

    @Test
    void testGetPersonalizedFeedWithInvalidPageSize() {
        given()
            .queryParam("travellerId", "test-user-123")
            .queryParam("pageSize", 200)
            .when()
            .get("/api/v1/feed")
            .then()
            .statusCode(400);
    }

    @Test
    void testGetPopularFeed() {
        given()
            .when()
            .get("/api/v1/feed/popular")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("items", notNullValue())
            .body("page", is(0))
            .body("pageSize", is(20));
    }

    @Test
    void testGetPopularFeedWithPagination() {
        given()
            .queryParam("page", 2)
            .queryParam("pageSize", 15)
            .when()
            .get("/api/v1/feed/popular")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("page", is(2))
            .body("pageSize", is(15));
    }
}

