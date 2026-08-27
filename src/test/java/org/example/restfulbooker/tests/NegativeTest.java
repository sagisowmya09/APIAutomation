package org.example.restfulbooker.tests;

import org.example.restfulbooker.models.Booking;
import org.example.restfulbooker.models.CreateBookingResponse;
import org.example.restfulbooker.models.TokenResponse;
import org.example.restfulbooker.support.BookingDataFactory;
import org.example.restfulbooker.support.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

@DisplayName("Negative / adversarial cases")
class NegativeTest extends BaseApiTest {

    @Test
    @DisplayName("POST /auth rejects invalid credentials")
    void auth_withBadCredentials_returnsBadCredentialsReason() {
        api.createToken("not-a-user", "wrong-password")
                .then()
                .statusCode(200) // API returns 200 with a reason body (known quirk)
                .body("token", nullValue())
                .body("reason", equalTo("Bad credentials"));
    }

    @Test
    @DisplayName("GET /booking/{id} for a non-existent id returns 404")
    void getBooking_nonExistentId_returns404() {
        api.getBooking(Integer.MAX_VALUE)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("PUT /booking/{id} without a token is forbidden")
    void putBooking_withoutToken_returns403() {
        CreateBookingResponse created = api.createBookingExpectingSuccess(BookingDataFactory.uniqueBooking());
        Booking update = BookingDataFactory.copyOf(created.getBooking());
        update.setFirstname("ShouldNotPersist");

        api.updateBookingWithoutAuth(created.getBookingid(), update)
                .then()
                .statusCode(403);

        // Confirm original data was not mutated
        api.getBooking(created.getBookingid())
                .then()
                .statusCode(200)
                .body("firstname", equalTo(created.getBooking().getFirstname()));
    }

    @Test
    @DisplayName("PUT /booking/{id} with a forged token is forbidden")
    void putBooking_withInvalidToken_returns403() {
        CreateBookingResponse created = api.createBookingExpectingSuccess(BookingDataFactory.uniqueBooking());
        Booking update = BookingDataFactory.copyOf(created.getBooking());
        update.setFirstname("ForgedTokenAttack");

        api.updateBooking(created.getBookingid(), update, "thisIsNotARealToken")
                .then()
                .statusCode(403);

        api.getBooking(created.getBookingid())
                .then()
                .statusCode(200)
                .body("firstname", equalTo(created.getBooking().getFirstname()));
    }

    @Test
    @DisplayName("DELETE /booking/{id} without a token is forbidden")
    void deleteBooking_withoutToken_returns403() {
        CreateBookingResponse created = api.createBookingExpectingSuccess(BookingDataFactory.uniqueBooking());

        api.deleteBookingWithoutAuth(created.getBookingid())
                .then()
                .statusCode(403);

        api.getBooking(created.getBookingid())
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("POST /booking with an empty body fails (server error / rejection)")
    void createBooking_emptyBody_isRejected() {
        // Restful Booker currently responds with 500 Internal Server Error for {}.
        // We assert it is NOT a successful create (no 200 with bookingid).
        api.createBooking(new Booking())
                .then()
                .statusCode(anyOf(is(400), is(500)));
    }

    @Test
    @DisplayName("PATCH with auth cannot resurrect a deleted booking")
    void patch_deletedBooking_doesNotSucceedAsUpdate() {
        CreateBookingResponse created = api.createBookingExpectingSuccess(BookingDataFactory.uniqueBooking());
        String token = api.createValidToken();

        api.deleteBooking(created.getBookingid(), token)
                .then()
                .statusCode(201);

        api.partialUpdateBooking(created.getBookingid(), Map.of("firstname", "Ghost"), token)
                .then()
                .statusCode(anyOf(is(404), is(405), is(400)));

        api.getBooking(created.getBookingid())
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Auth response for bad credentials is not mistaken for a usable token shape")
    void auth_badCredentials_hasNoUsableTokenField() {
        TokenResponse response = api.createToken("admin", "not-the-password")
                .then()
                .statusCode(200)
                .extract()
                .as(TokenResponse.class);

        org.junit.jupiter.api.Assertions.assertNull(response.getToken());
        org.junit.jupiter.api.Assertions.assertEquals("Bad credentials", response.getReason());
    }
}
