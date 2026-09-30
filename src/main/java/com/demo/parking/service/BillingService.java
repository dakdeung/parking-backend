package com.demo.parking.service;

import com.demo.parking.constant.ParameterValues;
import com.demo.parking.constant.ReservationStatus;
import com.demo.parking.entity.ParkingInvoice;
import com.demo.parking.entity.RateCard;
import com.demo.parking.entity.Reservation;
import com.demo.parking.exception.BusinessRuleViolationException;
import com.demo.parking.utils.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class BillingService {

    private final Clock clock;

    public BillingService(
        Clock clock
    ) {
        this.clock = clock;
    }

    public ParkingInvoice generateInvoice(
        Reservation reservation, 
        RateCard rateCard
    ) {
        if (!ReservationStatus.COMPLETED.equals(reservation.getStatus())
                || reservation.getActualStartTime() == null || reservation.getActualEndTime() == null) {
            throw new BusinessRuleViolationException("Invoice can only be generated for a COMPLETED reservation; "
                    + reservation.getReservationId() + " is " + reservation.getStatus());
        }

        if (!Objects.equals(reservation.getVehicleType(), rateCard.getVehicleType())) {
            throw new BusinessRuleViolationException("Rate card is for " + rateCard.getVehicleType()
                    + " but reservation is for " + reservation.getVehicleType());
        }

        var breakdown = calculateBreakdown(
            reservation.getActualStartTime(), 
            reservation.getActualEndTime(), 
            rateCard
        );

        var discount = Money.ZERO; 
        var total = Money.scale(breakdown.baseAmount()
                .add(breakdown.overnightSurcharge())
                .subtract(discount)
                .max(BigDecimal.ZERO));

        var invoice = new ParkingInvoice();
        invoice.setInvoiceId(ParameterValues.INVOICE_ID_PREFIX + UUID.randomUUID());
        invoice.setReservationId(reservation.getReservationId());
        invoice.setCustomerId(reservation.getCustomerId());
        invoice.setBilledDurationMinutes(breakdown.billedDurationMinutes());
        invoice.setBaseAmount(breakdown.baseAmount());
        invoice.setOvernightSurchargeApplied(breakdown.overnightSurcharge());
        invoice.setDiscountAmount(discount);
        invoice.setTotalAmount(total);
        invoice.setGeneratedAt(LocalDateTime.now(clock));
        return invoice;
    }

    public BigDecimal calculateAmount(
        LocalDateTime actualStart, 
        LocalDateTime actualEnd, 
        RateCard rateCard
    ) {
        var breakdown = calculateBreakdown(actualStart, actualEnd, rateCard);
        return Money.scale(
            breakdown.baseAmount().add(breakdown.overnightSurcharge())
        );
    }

    BillingBreakdown calculateBreakdown(
        LocalDateTime actualStart, 
        LocalDateTime actualEnd, 
        RateCard rateCard
    ) {
        if (actualEnd.isBefore(actualStart)) {
            throw new BusinessRuleViolationException("actualEnd must not be before actualStart");
        }

        var graceSeconds = rateCard.getGracePeriodMinutes() * ParameterValues.SECONDS_PER_MINUTE;
        var billableSeconds = Duration.between(actualStart, actualEnd).getSeconds() - graceSeconds;
        if (billableSeconds <= 0) {
            return BillingBreakdown.free();
        }

        var billingStart = actualStart.plusSeconds(graceSeconds);
        var blocks = ceilDiv(
            billableSeconds, 
            ParameterValues.BLOCK_SECONDS
        );
        var blockRate = rateCard.getHourlyRate().divide(ParameterValues.BLOCKS_PER_HOUR);

        var baseAmount = BigDecimal.ZERO;
        for (var day : blocksPerCalendarDay(billingStart, blocks).entrySet()) {
            var dayCharge = blockRate.multiply(BigDecimal.valueOf(day.getValue()));
            if (rateCard.getDailyCap() != null) {
                dayCharge = dayCharge.min(rateCard.getDailyCap());
            }
            baseAmount = baseAmount.add(dayCharge);
        }

        var midnightCrossings = countMidnightCrossings(
            actualStart, 
            actualEnd
        );
        var overnight = Money.orZero(rateCard.getOvernightSurcharge())
                .multiply(BigDecimal.valueOf(midnightCrossings));

        return new BillingBreakdown(
                ceilDiv(billableSeconds, ParameterValues.SECONDS_PER_MINUTE),
                Money.scale(baseAmount),
                Money.scale(overnight)
            );
    }

    private static Map<LocalDate, Long> blocksPerCalendarDay(
        LocalDateTime billingStart, 
        long blocks
    ) {
        var perDay = new TreeMap<LocalDate, Long>();
        for (var i = 0L; i < blocks; i++) {
            var day = billingStart.plusSeconds(i * ParameterValues.BLOCK_SECONDS).toLocalDate();
            perDay.merge(day, 1L, Long::sum);
        }
        return perDay;
    }

    static long countMidnightCrossings(
        LocalDateTime start, 
        LocalDateTime end
    ) {
        var days = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
        if (days > 0 && end.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            days--;
        }
        return Math.max(days, 0);
    }

    private static long ceilDiv(
        long dividend, 
        long divisor
    ) {
        return (dividend + divisor - 1) / divisor;
    }

    record BillingBreakdown(
        long billedDurationMinutes, 
        BigDecimal baseAmount, 
        BigDecimal overnightSurcharge
    ) {
        static BillingBreakdown free() {
            return new BillingBreakdown(
                0, 
                Money.ZERO, 
                Money.ZERO
            );
        }
    }
}
