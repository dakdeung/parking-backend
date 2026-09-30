package com.demo.parking.entity;

import com.demo.parking.constant.DefaultValues;
import com.demo.parking.constant.ParameterValues;
import com.demo.parking.converter.BooleanToNumberConverter;
import com.demo.parking.converter.NumberToNumberConverter;
import com.demo.parking.converter.StringToStingConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reservation")
@AllArgsConstructor
@NoArgsConstructor 
public class Reservation {

    @Id
    private String reservationId;
    @Convert(converter = StringToStingConverter.class)
    private String lotId = DefaultValues.EMPTY_STRING;
    @Convert(converter = StringToStingConverter.class)
    private String slotId = DefaultValues.EMPTY_STRING;
    @Convert(converter = NumberToNumberConverter.class)
    private int floorLevel = DefaultValues.EMPTY_INT;
    @Pattern(regexp = ParameterValues.VEHICLE_TYPE_REGEX)
    @Convert(converter = StringToStingConverter.class)
    private String vehicleType = DefaultValues.EMPTY_STRING;
    @Convert(converter = StringToStingConverter.class)
    private String licensePlate = DefaultValues.EMPTY_STRING;
    @Convert(converter = StringToStingConverter.class)
    private String customerId = DefaultValues.EMPTY_STRING;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    @Convert(converter = StringToStingConverter.class)
    private String status = DefaultValues.EMPTY_STRING;
    @Convert(converter = BooleanToNumberConverter.class)
    private boolean lateCancellation = Boolean.FALSE;
    @Convert(converter = StringToStingConverter.class)
    private String cancellationReason = DefaultValues.EMPTY_STRING;
    private LocalDateTime createdAt = LocalDateTime.now();
    @Version
    @Column(name = "row_version")
    private Long version;
}
