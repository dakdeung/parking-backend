package com.demo.parking.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.demo.parking.service.seeder.DataSeederService;

@Component
@ConditionalOnProperty(name = "parking.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final DataSeederService dataSeederService;

    public DataSeeder(DataSeederService dataSeederService) {
        this.dataSeederService = dataSeederService;
    }

    @Override
    public void run(String... args) {
        dataSeederService.seedAll();
    }
}
