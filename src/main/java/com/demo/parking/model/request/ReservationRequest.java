package com.demo.parking.model.request;

import java.time.LocalDateTime;

import com.demo.parking.constant.DefaultValues;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    @Schema(example = "LOT-JKT01")
    private String lotId = DefaultValues.EMPTY_STRING;
    @Schema(example = "CAR", allowableValues = {"MOTORCYCLE", "CAR", "TRUCK"})
    private String vehicleType = DefaultValues.EMPTY_STRING;
    @Schema(example = "B 1234 XYZ")
    private String licensePlate = DefaultValues.EMPTY_STRING;
    @Schema(example = "CUST-001")
    private String customerId = DefaultValues.EMPTY_STRING;
    @Schema(type = "string", example = "2024-03-15T09:00:00")
    private LocalDateTime plannedStartTime = LocalDateTime.now();
    @Schema(type = "string", example = "2024-03-15T13:00:00")
    private LocalDateTime plannedEndTime = LocalDateTime.now();
    @Schema(description = "Optional: book this exact slot instead of the best available one", example = "A-01", nullable = true)
    private String slotId = DefaultValues.EMPTY_STRING;

    @JsonIgnore
    @AssertTrue(message = "plannedEndTime must be after plannedStartTime")
    public boolean isPlannedEndTimeAfterStart() {
        return plannedStartTime == null || plannedEndTime == null || plannedEndTime.isAfter(plannedStartTime);
    }
}
