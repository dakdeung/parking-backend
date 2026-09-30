package com.demo.parking.converter;

import java.util.Optional;

import com.demo.parking.constant.DefaultValues;

import jakarta.persistence.AttributeConverter;

public class StringToStingConverter implements AttributeConverter<String, String>{

    @Override
    public String convertToDatabaseColumn(String input) {
        return Optional.ofNullable(input).orElse(DefaultValues.EMPTY_STRING);
    }

    @Override
    public String convertToEntityAttribute(String input) {
        return Optional.ofNullable(input).orElse(DefaultValues.EMPTY_STRING);
    }

}
