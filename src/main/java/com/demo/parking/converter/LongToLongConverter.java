package com.demo.parking.converter;

import java.util.Optional;

import com.demo.parking.constant.DefaultValues;

import jakarta.persistence.AttributeConverter;

public class LongToLongConverter implements AttributeConverter<Long, Long>{

    @Override
    public Long convertToDatabaseColumn(Long value) {
        return Optional.ofNullable(value).orElse(DefaultValues.EMPTY_LONG);
    }

    @Override
    public Long convertToEntityAttribute(Long value) {
        return Optional.ofNullable(value).orElse(DefaultValues.EMPTY_LONG);
    }
}
