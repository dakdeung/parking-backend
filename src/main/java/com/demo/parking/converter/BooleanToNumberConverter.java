package com.demo.parking.converter;

import java.util.Optional;

import jakarta.persistence.AttributeConverter;

public class BooleanToNumberConverter implements AttributeConverter<Boolean, Integer>{

    @Override
    public Integer convertToDatabaseColumn(Boolean input) {
        return Optional.ofNullable(input).filter(check -> check).map(check -> 1).orElse(0);
    }

    @Override
    public Boolean convertToEntityAttribute(Integer input) {
        return Optional.ofNullable(input).filter(check -> check == 1).map(check -> Boolean.TRUE).orElse(Boolean.FALSE);
    }
}
