package com.demo.parking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

import com.demo.parking.converter.LocalTimeStringConverter;

@Getter
@Setter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor 
public class OperatingHours {
    @Convert(converter = LocalTimeStringConverter.class)
    @Column(name = "open_time", length = 5, nullable = false)
    private LocalTime openTime;
    @Convert(converter = LocalTimeStringConverter.class)
    @Column(name = "close_time", length = 5, nullable = false)
    private LocalTime closeTime;
}
