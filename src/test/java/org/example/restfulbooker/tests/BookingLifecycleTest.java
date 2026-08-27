package org.example.restfulbooker.tests;

import io.restassured.module.jsv.JsonSchemaValidator;
import org.example.restfulbooker.models.Booking;
import org.example.restfulbooker.models.CreateBookingResponse;
import org.example.restfulbooker.support.BookingDataFactory;
import org.example.restfulbooker.support.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Booking lifecycle — create, read, update, delete")
class BookingLifecycleTest extends BaseApiTest {

    @Test
    @DisplayName("Create booking returns schema-valid payload that round-trips on GET")
    void createAndRetrieve_booking_roundTrips() {
        Booking request = BookingDataFactory.uniqueBooking();

        CreateBookingResponse created = api.createBooking(request)
                .then()
                .statusCode(200)
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath("schemas/booking-response-schema.json"))
                .extract()
                .as(CreateBookingResponse.class);

        assertThat(created.getBookingid(), greaterThan(0));
        assertTrue(request.sameAs(created.getBooking()),
                () -> "Create response booking mismatch. expected=" + request + " actual=" + created.getBooking());

        Booking fetched = api.getBooking(created.getBookingid())
                .then()
                .statusCode(200)
                .body("firstname", equalTo(request.getFirstname()))
                .body("lastname", equalTo(request.getLastname()))
                .body("totalprice", equalTo(request.getTotalprice()))
                .body("depositpaid", equalTo(request.getDepositpaid()))
                .body("bookingdates.checkin", equalTo(request.getBookingdates().getCheckin()))
                .body("bookingdates.checkout", equalTo(request.getBookingdates().getCheckout()))
                .body("additionalneeds", equalTo(request.getAdditionalneeds()))
                .extract()
                .as(Booking.class);

        assertTrue(request.sameAs(fetched),
                () -> "GET round-trip mismatch. expected=" + request + " actual=" + fetched);
    }

    @Test
    @DisplayName("PUT replaces the full booking and GET reflects every field")
    void updateBooking_fullPut_roundTrips() {
        Booking original = BookingDataFactory.uniqueBooking();
        CreateBookingResponse created = api.createBookingExpectingSuccess(original);
        String token = api.createValidToken();

        Booking updated = BookingDataFactory.copyOf(original);
        updated.setFirstname("Updated" + original.getFirstname());
        updated.setLastname("Updated" + original.getLastname());
        updated.setTotalprice(original.getTotalprice() + 42);
        updated.setDepositpaid(!Boolean.TRUE.equals(original.getDepositpaid()));
        updated.getBookingdates().setCheckout("2025-03-14");
        updated.setAdditionalneeds("Late checkout");

        Booking putResponse = api.updateBooking(created.getBookingid(), updated, token)
                .then()
                .statusCode(200)
                .body("firstname", equalTo(updated.getFirstname()))
                .body("lastname", equalTo(updated.getLastname()))
                .body("totalprice", equalTo(updated.getTotalprice()))
                .body("depositpaid", equalTo(updated.getDepositpaid()))
                .body("bookingdates.checkin", equalTo(updated.getBookingdates().getCheckin()))
                .body("bookingdates.checkout", equalTo(updated.getBookingdates().getCheckout()))
                .body("additionalneeds", equalTo(updated.getAdditionalneeds()))
                .extract()
                .as(Booking.class);

        assertTrue(updated.sameAs(putResponse));

        Booking fetched = api.getBooking(created.getBookingid())
                .then()
                .statusCode(200)
                .extract()
                .as(Booking.class);
        assertTrue(updated.sameAs(fetched),
                () -> "PUT did not persist. expected=" + updated + " actual=" + fetched);

        // cleanup
        api.deleteBooking(created.getBookingid(), token).then().statusCode(201);
    }

    @Test
    @DisplayName("PATCH updates only provided fields and leaves others intact")
    void updateBooking_partialPatch_preservesUntouchedFields() {
        Booking original = BookingDataFactory.uniqueBooking();
        CreateBookingResponse created = api.createBookingExpectingSuccess(original);
        String token = api.createValidToken();

        String patchedFirstName = "Patched" + original.getFirstname();

        Booking patchResponse = api.partialUpdateBooking(
                        created.getBookingid(),
                        Map.of("firstname", patchedFirstName),
                        token)
                .then()
                .statusCode(200)
                .body("firstname", equalTo(patchedFirstName))
                .body("lastname", equalTo(original.getLastname()))
                .body("totalprice", equalTo(original.getTotalprice()))
                .body("depositpaid", equalTo(original.getDepositpaid()))
                .body("bookingdates.checkin", equalTo(original.getBookingdates().getCheckin()))
                .body("bookingdates.checkout", equalTo(original.getBookingdates().getCheckout()))
                .body("additionalneeds", equalTo(original.getAdditionalneeds()))
                .extract()
                .as(Booking.class);

        assertThat(patchResponse.getFirstname(), equalTo(patchedFirstName));
        assertThat(patchResponse.getLastname(), equalTo(original.getLastname()));

        Booking fetched = api.getBooking(created.getBookingid())
                .then()
                .statusCode(200)
                .extract()
                .as(Booking.class);
        assertThat(fetched.getFirstname(), equalTo(patchedFirstName));
        assertThat(fetched.getLastname(), equalTo(original.getLastname()));
        assertThat(fetched.getTotalprice(), equalTo(original.getTotalprice()));

        api.deleteBooking(created.getBookingid(), token).then().statusCode(201);
    }

    @Test
    @DisplayName("DELETE removes the booking (GET then returns 404)")
    void deleteBooking_thenGet_returnsNotFound() {
        CreateBookingResponse created = api.createBookingExpectingSuccess(BookingDataFactory.uniqueBooking());
        String token = api.createValidToken();

        api.deleteBooking(created.getBookingid(), token)
                .then()
                .statusCode(201);

        api.getBooking(created.getBookingid())
                .then()
                .statusCode(404);
    }
}
