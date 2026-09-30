package com.demo.parking.service.seeder;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.parking.constant.VehicleType;


@Service
public class DataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);

    private final ParkingLotSeederService parkingLotSeederService;
    private final RateCardSeederService rateCardSeederService;

    public DataSeederService(
        ParkingLotSeederService parkingLotSeederService,
        RateCardSeederService rateCardSeederService
    ) {
        this.parkingLotSeederService = parkingLotSeederService;
        this.rateCardSeederService = rateCardSeederService;
    }

    @Transactional
    public void seedAll() {
        seedRateCards();
        seedLots();
    }

    private void seedRateCards() {

        if (!rateCardSeederService.isEmpty()) {
            return;
        }

        rateCardSeederService.save(
            rateCardSeederService.newRateCard(
                VehicleType.MOTORCYCLE,
                new BigDecimal("3000"), 
                new BigDecimal("20000"), 
                new BigDecimal("10000")
            )
        );
        
        rateCardSeederService.save(
            rateCardSeederService.newRateCard(
                VehicleType.CAR,
                new BigDecimal("5000"), 
                new BigDecimal("50000"), 
                new BigDecimal("20000")
            )
        );

        rateCardSeederService.save(
            rateCardSeederService.newRateCard(
                VehicleType.TRUCK,
                new BigDecimal("15000"), 
                new BigDecimal("120000"), 
                new BigDecimal("30000")
            )
        );

        log.info("Seeded rate cards for MOTORCYCLE, CAR, TRUCK");
    }

    private void seedLots() {

        if (!parkingLotSeederService.exists("LOT-JKT01")) {

            var jakarta = parkingLotSeederService.newLot(
                "LOT-JKT01", 
                "Parking Thamrin",
                "Jl. M.H. Thamrin No. 1, Jakarta", 
                LocalTime.MIDNIGHT, LocalTime.MIDNIGHT
            );

            for (var i = 1; i <= 4; i++) {
                parkingLotSeederService.addSlot(
                    jakarta, String.format("M-%02d", i), 
                    VehicleType.MOTORCYCLE, 
                    0
                );
            }

            for (var i = 1; i <= 3; i++) {
                parkingLotSeederService.addSlot(
                    jakarta, String.format("A-%02d", i), 
                    VehicleType.CAR, 0
                );
                parkingLotSeederService.addSlot(
                    jakarta, String.format("B-%02d", i), 
                    VehicleType.CAR, 1
                );
            }
            
            parkingLotSeederService.addSlot(
                jakarta, "T-01", 
                VehicleType.TRUCK, 
                0
            );

            parkingLotSeederService.addSlot(
                jakarta, 
                "T-02", 
                VehicleType.TRUCK, 
                0
            );

            parkingLotSeederService.save(jakarta);
            log.info("Seeded parking lot LOT-JKT01");
        }

        if (!parkingLotSeederService.exists("LOT-BDG01")) {

            var bandung = parkingLotSeederService.newLot(
                "LOT-BDG01", "Parking Dago",
                "Jl. Ir. H. Juanda, Bandung", 
                LocalTime.of(6, 0), 
                LocalTime.of(22, 0)
            );

            parkingLotSeederService.addSlot(
                bandung, 
                "A-01", 
                VehicleType.CAR, 
                0
            );

            parkingLotSeederService.addSlot(
                bandung, 
                "A-02", 
                VehicleType.CAR, 
                0
            );
            
            parkingLotSeederService.addSlot(
                bandung, "M-01", 
                VehicleType.MOTORCYCLE, 
                0
            );
            
            parkingLotSeederService.save(bandung);
            log.info("Seeded parking lot LOT-BDG01");
        }
    }
}
