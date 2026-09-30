package com.demo.parking.model.request;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.demo.parking.constant.DefaultValues;
import com.demo.parking.constant.ParameterValues;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityRequest {

    @Pattern(regexp = ParameterValues.VEHICLE_TYPE_REGEX, message = "must be MOTORCYCLE, CAR or TRUCK")
    @Schema(example = "CAR", allowableValues = {"MOTORCYCLE", "CAR", "TRUCK"})
    private String vehicleType = DefaultValues.EMPTY_STRING;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(type = "string", example = "2024-03-15T09:00:00")
    private LocalDateTime startTime = LocalDateTime.now();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(type = "string", example = "2024-03-15T13:00:00")
    private LocalDateTime endTime = LocalDateTime.now();

    public boolean isEmpty() {
        return vehicleType == null && startTime == null && endTime == null;
    }
}
