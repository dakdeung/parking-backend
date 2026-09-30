package com.demo.parking.constant;

public final class UrlValues {

    public static final String API_V1 = "/api/v1";
    public static final String LOTS = API_V1 + "/lots";
    public static final String RESERVATIONS = API_V1 + "/reservations";
    public static final String INVOICES = API_V1 + "/invoices";

    public static final String AVAILABILITY = "/{lotId}/availability";
    public static final String RESERVATION_ID = "/{reservationId}";
    public static final String CHECK_IN = "/{reservationId}/check-in";
    public static final String CHECK_OUT = "/{reservationId}/check-out";
    public static final String EXTEND = "/{reservationId}/extend";
}
