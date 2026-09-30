package com.demo.parking.converter;

import java.util.Optional;

import com.demo.parking.constant.DefaultValues;

import jakarta.persistence.AttributeConverter;

public class NumberToNumberConverter implements AttributeConverter<Integer, Integer>{

    @Override
    public Integer convertToDatabaseColumn(Integer integer) {
        return Optional.ofNullable(integer).orElse(DefaultValues.EMPTY_INT);
    }

    @Override
    public Integer convertToEntityAttribute(Integer integer) {
        return Optional.ofNullable(integer).orElse(DefaultValues.EMPTY_INT);
    }

}
