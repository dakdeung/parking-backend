package com.demo.parking.service.seeder;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.parking.constant.Lengths;
import com.demo.parking.constant.VehicleType;
import com.demo.parking.entity.RateCard;
import com.demo.parking.exception.InvalidRequestException;
import com.demo.parking.repository.RateCardRepository;

@Service
@Transactional
public class RateCardSeederService {

    private final RateCardRepository rateCardRepository;

    public RateCardSeederService(RateCardRepository rateCardRepository) {
        this.rateCardRepository = rateCardRepository;
    }

    @Transactional(readOnly = true)
    public boolean isEmpty() {
        return rateCardRepository.count() == 0;
    }

    public RateCard save(
        RateCard rateCard
    ) {
        return rateCardRepository.save(rateCard);
    }

    public RateCard newRateCard(
        String vehicleType, 
        BigDecimal hourlyRate, 
        BigDecimal dailyCap,
        BigDecimal overnightSurcharge, 
        int gracePeriodMinutes
    ) {
        if (vehicleType == null || !VehicleType.VALUES.contains(vehicleType)) {
            throw new InvalidRequestException("vehicleType", "Unknown vehicle type " + vehicleType);
        }

        if (hourlyRate == null) {
            throw new InvalidRequestException("hourlyRate", "hourlyRate must not be null");
        }

        requireNonNegative(hourlyRate, "hourlyRate");
        requireNonNegative(dailyCap, "dailyCap");
        requireNonNegative(overnightSurcharge, "overnightSurcharge");

        if (gracePeriodMinutes < 0) {
            throw new InvalidRequestException("gracePeriodMinutes", "gracePeriodMinutes must be >= 0");
        }

        var rateCard = new RateCard();
        rateCard.setVehicleType(vehicleType);
        rateCard.setHourlyRate(hourlyRate);
        rateCard.setDailyCap(dailyCap);
        rateCard.setOvernightSurcharge(overnightSurcharge);
        rateCard.setGracePeriodMinutes(gracePeriodMinutes);
        return rateCard;
    }

    public RateCard newRateCard(
        String vehicleType, 
        BigDecimal hourlyRate, 
        BigDecimal dailyCap,
        BigDecimal overnightSurcharge
    ) {
        return newRateCard(
            vehicleType, 
            hourlyRate, 
            dailyCap, 
            overnightSurcharge, 
            Lengths.GRACE_PERIOD_MINUTES
        );
    }

    private static void requireNonNegative(
        BigDecimal value, 
        String field
    ) {
        if (value != null && value.signum() < 0) {
            throw new InvalidRequestException(field, field + " must not be negative");
        }
    }
}
