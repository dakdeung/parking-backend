package com.demo.parking.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI parkingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Parking API")
                .version("v1")
                .description("Parking lot reservation & billing system."));
    }
}
