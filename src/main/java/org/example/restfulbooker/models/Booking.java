package org.example.restfulbooker.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Booking {

    private String firstname;
    private String lastname;
    private Integer totalprice;
    private Boolean depositpaid;
    private BookingDates bookingdates;
    private String additionalneeds;

    public Booking() {
    }

    public Booking(String firstname,
                   String lastname,
                   Integer totalprice,
                   Boolean depositpaid,
                   BookingDates bookingdates,
                   String additionalneeds) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.totalprice = totalprice;
        this.depositpaid = depositpaid;
        this.bookingdates = bookingdates;
        this.additionalneeds = additionalneeds;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Integer getTotalprice() {
        return totalprice;
    }

    public void setTotalprice(Integer totalprice) {
        this.totalprice = totalprice;
    }

    public Boolean getDepositpaid() {
        return depositpaid;
    }

    public void setDepositpaid(Boolean depositpaid) {
        this.depositpaid = depositpaid;
    }

    public BookingDates getBookingdates() {
        return bookingdates;
    }

    public void setBookingdates(BookingDates bookingdates) {
        this.bookingdates = bookingdates;
    }

    public String getAdditionalneeds() {
        return additionalneeds;
    }

    public void setAdditionalneeds(String additionalneeds) {
        this.additionalneeds = additionalneeds;
    }

    /**
     * Field-by-field equality used for round-trip assertions.
     */
    public boolean sameAs(Booking other) {
        if (other == null) {
            return false;
        }
        return Objects.equals(firstname, other.firstname)
                && Objects.equals(lastname, other.lastname)
                && Objects.equals(totalprice, other.totalprice)
                && Objects.equals(depositpaid, other.depositpaid)
                && Objects.equals(additionalneeds, other.additionalneeds)
                && Objects.equals(
                bookingdates == null ? null : bookingdates.getCheckin(),
                other.bookingdates == null ? null : other.bookingdates.getCheckin())
                && Objects.equals(
                bookingdates == null ? null : bookingdates.getCheckout(),
                other.bookingdates == null ? null : other.bookingdates.getCheckout());
    }

    @Override
    public String toString() {
        return "Booking{firstname='%s', lastname='%s', totalprice=%s, depositpaid=%s, bookingdates=%s/%s, additionalneeds='%s'}"
                .formatted(
                        firstname,
                        lastname,
                        totalprice,
                        depositpaid,
                        bookingdates == null ? null : bookingdates.getCheckin(),
                        bookingdates == null ? null : bookingdates.getCheckout(),
                        additionalneeds);
    }
}
