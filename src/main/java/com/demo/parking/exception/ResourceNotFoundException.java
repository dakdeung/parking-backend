package com.demo.parking.exception;

public class ResourceNotFoundException extends ParkingException {

    public ResourceNotFoundException(String resource, String id) {
        super(resource + " '" + id + "' was not found");
    }
}
