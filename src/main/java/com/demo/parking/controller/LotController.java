package com.demo.parking.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.parking.constant.UrlValues;
import com.demo.parking.model.request.AvailabilityRequest;
import com.demo.parking.model.response.AvailableSlotResponse;
import com.demo.parking.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping(UrlValues.LOTS)
public class LotController {

    private final ReservationService reservationService;

    public LotController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping(UrlValues.AVAILABILITY)
    @Operation(summary = "List available slots for a vehicle type and time window (best slot first)")
    public List<AvailableSlotResponse> availability(
            @PathVariable String lotId,
            @Valid @RequestBody AvailabilityRequest request
    ) {
        return reservationService.findAvailableSlots(
            lotId,
            request
        );
    }
}
