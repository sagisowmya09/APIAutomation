package org.example.restfulbooker.tests;

import org.example.restfulbooker.client.RestfulBookerClient;
import org.example.restfulbooker.config.TestConfig;
import org.example.restfulbooker.models.TokenResponse;
import org.example.restfulbooker.support.BaseApiTest;
import org.example.restfulbooker.validation.ResponseValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Auth — obtain and validate tokens")
class AuthTest extends BaseApiTest {

    @Test
    @DisplayName("POST /auth with valid credentials returns a non-blank token")
    void createToken_withValidCredentials_returnsToken() {
        var rawResponse = api.createToken(TestConfig.authUsername(), TestConfig.authPassword());
        ResponseValidator.statusCode(rawResponse, 200);
        ResponseValidator.fieldIsUsable(rawResponse, "token");
        TokenResponse response = rawResponse.as(TokenResponse.class);

        assertThat(response.getToken(), matchesPattern("^[a-zA-Z0-9]+$"));
        assertThat(response.getToken().length(), greaterThanOrEqualTo(10));
    }

    @Test
    @DisplayName("Token from /auth can authorize a protected DELETE")
    void createToken_canBeUsedForProtectedEndpoint() {
        var created = api.createBookingExpectingSuccess(
                org.example.restfulbooker.support.BookingDataFactory.uniqueBooking());
        String token = api.createValidToken();

        api.deleteBooking(created.getBookingid(), token)
                .then()
                .statusCode(201);

        api.getBooking(created.getBookingid())
                .then()
                .statusCode(404);
    }
}
