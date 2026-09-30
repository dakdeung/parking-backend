package com.demo.parking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.demo.parking.constant.DefaultValues;
import com.demo.parking.converter.BigDecimalToBigDecimalConverter;
import com.demo.parking.converter.LongToLongConverter;
import com.demo.parking.converter.StringToStingConverter;


@Getter
@Setter
@Entity
@Table(name = "parking_invoice")
@AllArgsConstructor
@NoArgsConstructor 
public class ParkingInvoice {
    @Id
    private String invoiceId;
    @Convert(converter = StringToStingConverter.class)
    private String reservationId = DefaultValues.EMPTY_STRING;
    @Convert(converter = StringToStingConverter.class)
    private String customerId = DefaultValues.EMPTY_STRING;
    @Convert(converter = LongToLongConverter.class)
    private long billedDurationMinutes = DefaultValues.EMPTY_LONG;
    @Convert(converter = BigDecimalToBigDecimalConverter.class)
    private BigDecimal baseAmount = BigDecimal.ZERO;
    @Convert(converter = BigDecimalToBigDecimalConverter.class)
    private BigDecimal overnightSurchargeApplied = BigDecimal.ZERO;
    @Convert(converter = BigDecimalToBigDecimalConverter.class)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Convert(converter = BigDecimalToBigDecimalConverter.class)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private LocalDateTime generatedAt;
    @Version
    @Column(name = "row_version")
    private Long version;
}
