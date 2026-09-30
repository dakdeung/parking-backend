package com.demo.parking.converter;

import java.time.LocalTime;

import jakarta.persistence.AttributeConverter;

public class LocalTimeStringConverter implements AttributeConverter<LocalTime, String> {

    @Override
    public String convertToDatabaseColumn(LocalTime attribute) {
        return attribute == null ? null : attribute.toString().substring(0, 5);
    }

    @Override
    public LocalTime convertToEntityAttribute(String string) {
        return string == null ? null : LocalTime.parse(string);
    }
}
