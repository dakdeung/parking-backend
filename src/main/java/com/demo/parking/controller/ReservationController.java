package com.demo.parking.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.parking.constant.UrlValues;
import com.demo.parking.model.request.CancelReservationRequest;
import com.demo.parking.model.request.CheckInRequest;
import com.demo.parking.model.request.CheckOutRequest;
import com.demo.parking.model.request.ExtendReservationRequest;
import com.demo.parking.model.request.ReservationRequest;
import com.demo.parking.model.response.CheckOutResponse;
import com.demo.parking.model.response.ReservationResponse;
import com.demo.parking.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping(UrlValues.RESERVATIONS)
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Create a reservation (201). 409 when no slot is free for the window.")
    public ReservationResponse create(
        @Valid @RequestBody ReservationRequest request
    ) {
        return reservationService.createReservation(
            request
        );
    }

    @GetMapping(UrlValues.RESERVATION_ID)
    @Operation(summary = "Get a reservation")
    public ReservationResponse get(
        @PathVariable String reservationId
    ) {
        return reservationService.getReservation(
            reservationId
        );
    }

    @PostMapping(UrlValues.CHECK_IN)
    @Operation(summary = "Check in. More than 30 min after plannedStartTime the reservation becomes NO_SHOW.")
    public ReservationResponse checkIn(
        @PathVariable String reservationId,
        @Valid @RequestBody CheckInRequest request
    ) {
        return reservationService.checkIn(
            reservationId, 
            request.getActualStartTime()
        );
    }

    @PostMapping(UrlValues.CHECK_OUT)
    @Operation(summary = "Check out; returns the completed reservation and its invoice")
    public CheckOutResponse checkOut(
        @PathVariable String reservationId,
        @Valid @RequestBody CheckOutRequest request
    ) {
        return reservationService.checkOut(
            reservationId, request.getActualEndTime()
        );
    }

    @DeleteMapping(UrlValues.RESERVATION_ID)
    @Operation(summary = "Cancel a PENDING or ACTIVE reservation (sets lateCancellation within 30 min of start)")
    public ReservationResponse cancel(
        @PathVariable String reservationId,
        @Valid @RequestBody(required = false) CancelReservationRequest request
    ) {
        return reservationService.cancelReservation(
            reservationId, 
            request
        );
    }

    @PutMapping(UrlValues.EXTEND)
    @Operation(summary = "Extend a reservation. 409 when the slot is booked in the extra window.")
    public ReservationResponse extend(
        @PathVariable String reservationId,
        @Valid @RequestBody ExtendReservationRequest request
    ) {
        return reservationService.extendReservation(
            reservationId, 
            request.getNewEndTime()
        );
    }
}
