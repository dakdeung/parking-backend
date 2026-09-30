package com.demo.parking.entity;

import com.demo.parking.constant.DefaultValues;
import com.demo.parking.constant.ParameterValues;
import com.demo.parking.converter.NumberToNumberConverter;
import com.demo.parking.converter.StringToStingConverter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.UuidGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "parking_slot")
@AllArgsConstructor
@NoArgsConstructor 
public class ParkingSlot {

    @Id
    @UuidGenerator
    private String id;
    @Pattern(regexp = ParameterValues.SLOT_ID_REGEX)
    @Convert(converter = StringToStingConverter.class)
    private String slotId = DefaultValues.EMPTY_STRING;
    @Pattern(regexp = ParameterValues.VEHICLE_TYPE_REGEX)
    @Convert(converter = StringToStingConverter.class)
    private String vehicleType = DefaultValues.EMPTY_STRING;
    @Convert(converter = StringToStingConverter.class)
    private String status = DefaultValues.EMPTY_STRING;
    @Convert(converter = NumberToNumberConverter.class)
    private int floorLevel = DefaultValues.EMPTY_INT;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "lot_id", 
        nullable = false, 
        updatable = false
    )
    private ParkingLot parkingLot;
}
