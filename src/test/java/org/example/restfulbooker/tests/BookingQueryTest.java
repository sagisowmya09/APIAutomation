package org.example.restfulbooker.tests;

import org.example.restfulbooker.models.Booking;
import org.example.restfulbooker.models.BookingId;
import org.example.restfulbooker.models.CreateBookingResponse;
import org.example.restfulbooker.support.BookingDataFactory;
import org.example.restfulbooker.support.BaseApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Querying — filter bookings by name and date")
class BookingQueryTest extends BaseApiTest {

    @Test
    @DisplayName("GET /booking?firstname&lastname returns the booking we just created")
    void filterByName_returnsCreatedBooking() {
        Booking booking = BookingDataFactory.uniqueBooking();
        CreateBookingResponse created = api.createBookingExpectingSuccess(booking);

        List<BookingId> matches = Arrays.asList(
                api.getBookings(Map.of(
                                "firstname", booking.getFirstname(),
                                "lastname", booking.getLastname()))
                        .then()
                        .statusCode(200)
                        .body("$", not(empty()))
                        .extract()
                        .as(BookingId[].class)
        );

        Set<Integer> ids = matches.stream().map(BookingId::getBookingid).collect(Collectors.toSet());
        assertThat("Filtered name query should include the created booking id",
                ids, hasItem(created.getBookingid()));

        // Round-trip: every returned id should actually carry the filtered name
        for (Integer id : ids) {
            api.getBooking(id)
                    .then()
                    .statusCode(200)
                    .body("firstname", equalTo(booking.getFirstname()))
                    .body("lastname", equalTo(booking.getLastname()));
        }
    }

    @Test
    @DisplayName("GET /booking?checkin&checkout returns booking created within the date window")
    void filterByDate_returnsCreatedBooking() {
        // Use a distinctive future window unlikely to collide with seed data.
        String checkin = "2030-11-10";
        String checkout = "2030-11-15";
        Booking booking = BookingDataFactory.uniqueBookingWithDates(checkin, checkout);
        CreateBookingResponse created = api.createBookingExpectingSuccess(booking);

        // Restful Booker date filter semantics: bookings with checkin date greater than or equal
        // to the query checkin (and similarly for checkout). We assert our created id appears.
        List<BookingId> matches = Arrays.asList(
                api.getBookings(Map.of(
                                "checkin", "2030-11-01",
                                "checkout", "2030-12-01"))
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(BookingId[].class)
        );

        Set<Integer> ids = matches.stream().map(BookingId::getBookingid).collect(Collectors.toSet());
        assertThat("Date filter should include the booking created in-window",
                ids, hasItem(created.getBookingid()));

        Booking fetched = api.getBooking(created.getBookingid())
                .then()
                .statusCode(200)
                .extract()
                .as(Booking.class);
        assertThat(fetched.getBookingdates().getCheckin(), equalTo(checkin));
        assertThat(fetched.getBookingdates().getCheckout(), equalTo(checkout));
    }

    @Test
    @DisplayName("GET /booking with unknown name returns an empty list (not an error)")
    void filterByName_unknownGuest_returnsEmptyList() {
        api.getBookings(Map.of(
                        "firstname", "NoSuchGuestZzZz",
                        "lastname", "DefinitelyMissingYyYy"))
                .then()
                .statusCode(200)
                .body("$", empty());
    }
}
