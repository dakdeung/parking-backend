package com.demo.parking.exception;

public abstract class ParkingException extends RuntimeException {

    protected ParkingException(String message) {
        super(message);
    }
}
