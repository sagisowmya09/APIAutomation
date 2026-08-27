package org.example.restfulbooker.client;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.example.restfulbooker.config.TestConfig;
import org.example.restfulbooker.models.Booking;
import org.example.restfulbooker.models.CreateBookingResponse;
import org.example.restfulbooker.models.TokenResponse;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Thin REST client wrapping Restful Booker endpoints.
 * Keeps HTTP details out of test methods so assertions stay readable.
 * Timeouts are configured for slower CI/Jenkins networks as well as local runs.
 */
public class RestfulBookerClient {

    private final RequestSpecification jsonSpec;

    public RestfulBookerClient() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        RestAssuredConfig httpConfig = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", TestConfig.connectTimeoutMs())
                        .setParam("http.socket.timeout", TestConfig.readTimeoutMs()));

        // Restful Booker returns 418 if Accept is not exactly "application/json"
        // (RestAssured ContentType.JSON expands to a multi-value Accept list).
        this.jsonSpec = new RequestSpecBuilder()
                .setBaseUri(TestConfig.baseUri())
                .setContentType(ContentType.JSON)
                .setAccept("application/json")
                .setConfig(httpConfig)
                .addFilter(new RequestLoggingFilter(LogDetail.METHOD))
                .addFilter(new RequestLoggingFilter(LogDetail.URI))
                .addFilter(new ResponseLoggingFilter(LogDetail.STATUS))
                .build();
    }

    public Response ping() {
        return given().spec(jsonSpec).when().get("/ping");
    }

    public Response createToken(String username, String password) {
        return given()
                .spec(jsonSpec)
                .body(Map.of("username", username, "password", password))
                .when()
                .post("/auth");
    }

    public String createValidToken() {
        TokenResponse tokenResponse = createToken(TestConfig.authUsername(), TestConfig.authPassword())
                .then()
                .statusCode(200)
                .extract()
                .as(TokenResponse.class);
        if (tokenResponse.getToken() == null || tokenResponse.getToken().isBlank()) {
            throw new IllegalStateException("Auth succeeded but token was blank");
        }
        return tokenResponse.getToken();
    }

    public Response createBooking(Booking booking) {
        return given()
                .spec(jsonSpec)
                .body(booking)
                .when()
                .post("/booking");
    }

    public CreateBookingResponse createBookingExpectingSuccess(Booking booking) {
        return createBooking(booking)
                .then()
                .statusCode(200)
                .extract()
                .as(CreateBookingResponse.class);
    }

    public Response getBooking(int bookingId) {
        return given()
                .spec(jsonSpec)
                .when()
                .get("/booking/{id}", bookingId);
    }

    public Response getBookings(Map<String, ?> queryParams) {
        return given()
                .spec(jsonSpec)
                .queryParams(queryParams)
                .when()
                .get("/booking");
    }

    public Response updateBooking(int bookingId, Booking booking, String token) {
        return given()
                .spec(jsonSpec)
                .cookie("token", token)
                .body(booking)
                .when()
                .put("/booking/{id}", bookingId);
    }

    public Response updateBookingWithoutAuth(int bookingId, Booking booking) {
        return given()
                .spec(jsonSpec)
                .body(booking)
                .when()
                .put("/booking/{id}", bookingId);
    }

    public Response partialUpdateBooking(int bookingId, Map<String, ?> patchBody, String token) {
        return given()
                .spec(jsonSpec)
                .cookie("token", token)
                .body(patchBody)
                .when()
                .patch("/booking/{id}", bookingId);
    }

    public Response deleteBooking(int bookingId, String token) {
        return given()
                .spec(jsonSpec)
                .cookie("token", token)
                .when()
                .delete("/booking/{id}", bookingId);
    }

    public Response deleteBookingWithoutAuth(int bookingId) {
        return given()
                .spec(jsonSpec)
                .when()
                .delete("/booking/{id}", bookingId);
    }
}
