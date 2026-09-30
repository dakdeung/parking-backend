package com.demo.parking.model.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    private String reservationId;
    private String lotId;
    private String slotId;
    private int floorLevel;
    private String vehicleType;
    private String licensePlate;
    private String customerId;
    private String status;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private boolean lateCancellation;
    private String cancellationReason;
}
