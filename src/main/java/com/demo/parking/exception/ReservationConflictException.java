package com.demo.parking.exception;

public class ReservationConflictException extends ParkingException {

    public ReservationConflictException(String message) {
        super(message);
    }
}
