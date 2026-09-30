package com.demo.parking.exception;

public class BusinessRuleViolationException extends ParkingException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
