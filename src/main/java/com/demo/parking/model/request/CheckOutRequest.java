package com.demo.parking.model.request;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckOutRequest {
    @Schema(type = "string", example = "2024-03-15T12:45:00")
    private LocalDateTime actualEndTime = LocalDateTime.now();
}
