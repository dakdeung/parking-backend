package com.demo.parking.converter;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.persistence.AttributeConverter;

public class BigDecimalToBigDecimalConverter implements AttributeConverter<BigDecimal, BigDecimal>{

    @Override
    public BigDecimal convertToDatabaseColumn(BigDecimal bigDecimal) {
        return Optional.ofNullable(bigDecimal).orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal convertToEntityAttribute(BigDecimal bigDecimal) {
        return Optional.ofNullable(bigDecimal).orElse(BigDecimal.ZERO);
    }

}
