package com.demo.parking.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotResponse {
    private String slotId;
    private int floorLevel;
    private String vehicleType;
    private String currentStatus;
}
