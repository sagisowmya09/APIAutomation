package org.example.restfulbooker.support;

import org.example.restfulbooker.models.Booking;
import org.example.restfulbooker.models.BookingDates;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds unique booking payloads so tests do not collide when run in any order / in parallel.
 */
public final class BookingDataFactory {

    private BookingDataFactory() {
    }

    public static Booking uniqueBooking() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return new Booking(
                "First" + suffix,
                "Last" + suffix,
                ThreadLocalRandom.current().nextInt(50, 900),
                true,
                new BookingDates("2025-03-01", "2025-03-07"),
                "Breakfast"
        );
    }

    public static Booking uniqueBookingWithDates(String checkin, String checkout) {
        Booking booking = uniqueBooking();
        booking.setBookingdates(new BookingDates(checkin, checkout));
        return booking;
    }

    public static Booking copyOf(Booking source) {
        return new Booking(
                source.getFirstname(),
                source.getLastname(),
                source.getTotalprice(),
                source.getDepositpaid(),
                new BookingDates(
                        source.getBookingdates().getCheckin(),
                        source.getBookingdates().getCheckout()),
                source.getAdditionalneeds()
        );
    }
}
