package com.demo.parking.entity;

import com.demo.parking.constant.DefaultValues;
import com.demo.parking.constant.Lengths;
import com.demo.parking.constant.ParameterValues;
import com.demo.parking.converter.BigDecimalToBigDecimalConverter;
import com.demo.parking.converter.NumberToNumberConverter;
import com.demo.parking.converter.StringToStingConverter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.UuidGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "rate_card")
@AllArgsConstructor
@NoArgsConstructor 
public class RateCard {

    @Id
    @UuidGenerator
    private String id;
    @Pattern(regexp = ParameterValues.VEHICLE_TYPE_REGEX)
    @Convert(converter = StringToStingConverter.class)
    private String vehicleType = DefaultValues.EMPTY_STRING;
    @Convert(converter = BigDecimalToBigDecimalConverter.class)
    private BigDecimal hourlyRate = BigDecimal.ZERO;
    private BigDecimal dailyCap;
    @Convert(converter = BigDecimalToBigDecimalConverter.class)
    private BigDecimal overnightSurcharge = BigDecimal.ZERO;
    @Convert(converter = NumberToNumberConverter.class)
    private int gracePeriodMinutes = Lengths.GRACE_PERIOD_MINUTES;
}
