package com.demo.parking.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfiguration {

    public static final DateTimeFormatter API_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer parkingJacksonCustomizer() {
        return builder -> builder.serializers(
                new LocalDateTimeSerializer(API_DATE_TIME),
                new BigDecimalAsStringSerializer());
    }

    static final class BigDecimalAsStringSerializer extends StdSerializer<BigDecimal> {

        BigDecimalAsStringSerializer() {
            super(BigDecimal.class);
        }

        @Override
        public void serialize(BigDecimal value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString(value.toPlainString());
        }
    }
}
