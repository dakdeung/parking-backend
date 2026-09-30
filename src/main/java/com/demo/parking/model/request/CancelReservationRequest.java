package com.demo.parking.model.request;

import com.demo.parking.constant.DefaultValues;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelReservationRequest {
    @Schema(example = "change of plans")
    private String reason = DefaultValues.EMPTY_STRING;
}
