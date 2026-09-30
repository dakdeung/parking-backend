package com.demo.parking.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.demo.parking.constant.ReservationStatus;
import com.demo.parking.constant.VehicleType;
import com.demo.parking.entity.ParkingInvoice;
import com.demo.parking.entity.RateCard;
import com.demo.parking.entity.Reservation;
import com.demo.parking.exception.BusinessRuleViolationException;
import com.demo.parking.service.seeder.RateCardSeederService;

class BillingServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-03-20T00:00:00Z"), ZoneOffset.UTC);

    private final BillingService billingService = new BillingService(CLOCK);

    private final RateCardSeederService rateCardSeeder = new RateCardSeederService(null);

    private final RateCard CAR = rateCardSeeder.newRateCard(VehicleType.CAR,
            new BigDecimal("5000"), new BigDecimal("50000"), new BigDecimal("20000"));
    private final RateCard TRUCK = rateCardSeeder.newRateCard(VehicleType.TRUCK,
            new BigDecimal("15000"), new BigDecimal("120000"), new BigDecimal("30000"));
    private final RateCard MOTORCYCLE = rateCardSeeder.newRateCard(VehicleType.MOTORCYCLE,
            new BigDecimal("3000"), null, null);

    private static LocalDateTime at(String isoDateTime) {
        return LocalDateTime.parse(isoDateTime);
    }

    @Nested
    @DisplayName("Reference cases from the specification")
    class ReferenceCases {

        @Test
        @DisplayName("Case A: 09:00-12:45 CAR = 7 blocks x 2,500 = 17,500.00")
        void standardHourlyBillingWithoutCapOrOvernight() {
            BigDecimal amount = billingService.calculateAmount(at("2024-03-15T09:00:00"), at("2024-03-15T12:45:00"), CAR);

            assertThat(amount).isEqualByComparingTo("17500.00");
            assertThat(amount.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Case B: TRUCK 15 Mar 07:00 - 16 Mar 09:00 = 120,000 + 30,000 + 120,000 = 270,000.00")
        void multiDayStayAppliesDailyCapAndOvernightSurcharge() {
            BigDecimal amount = billingService.calculateAmount(at("2024-03-15T07:00:00"), at("2024-03-16T09:00:00"), TRUCK);

            assertThat(amount).isEqualByComparingTo("270000.00");
        }

        @Test
        @DisplayName("Case C: 12 minute MOTORCYCLE stay is inside the grace period = 0.00")
        void stayWithinGracePeriodIsFree() {
            BigDecimal amount = billingService.calculateAmount(at("2024-03-15T14:00:00"), at("2024-03-15T14:12:00"), MOTORCYCLE);

            assertThat(amount).isEqualByComparingTo("0.00");
            assertThat(amount.scale()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Exactly 60 minutes after grace = 2 blocks, no rounding up")
        void exactThirtyMinuteBoundaryIsNotRoundedUp() {
            BigDecimal amount = billingService.calculateAmount(at("2024-03-15T09:00:00"), at("2024-03-15T10:15:00"), CAR);

            assertThat(amount).isEqualByComparingTo("5000.00");
        }

        @Test
        @DisplayName("One minute over a boundary rounds up to the next block")
        void partialBlockIsRoundedUp() {
            BigDecimal amount = billingService.calculateAmount(at("2024-03-15T09:00:00"), at("2024-03-15T10:16:00"), CAR);

            assertThat(amount).isEqualByComparingTo("7500.00");
        }

        @Test
        @DisplayName("Stay equal to the grace period is free")
        void stayExactlyEqualToGracePeriodIsFree() {
            BigDecimal amount = billingService.calculateAmount(at("2024-03-15T09:00:00"), at("2024-03-15T09:15:00"), CAR);

            assertThat(amount).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("3-day stay crosses two midnights: surcharge applied twice")
        void twoMidnightCrossingsApplySurchargeTwice() {
            BigDecimal amount = billingService.calculateAmount(at("2024-03-15T20:00:00"), at("2024-03-17T08:00:00"), CAR);

            assertThat(amount).isEqualByComparingTo("150000.00");
        }

        @Test
        @DisplayName("Leaving exactly at midnight is not an overnight stay")
        void checkOutExactlyAtMidnightHasNoSurcharge() {
            BigDecimal amount = billingService.calculateAmount(at("2024-03-15T22:00:00"), at("2024-03-16T00:00:00"), CAR);

            assertThat(amount).isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("Odd hourly rate keeps exact BigDecimal precision (HALF_UP, 2 dp)")
        void oddHourlyRateIsRoundedHalfUp() {
            var odd = rateCardSeeder.newRateCard(VehicleType.CAR, new BigDecimal("3333.33"), null, null);

            BigDecimal amount = billingService.calculateAmount(at("2024-03-15T09:00:00"), at("2024-03-15T09:45:00"), odd);

            assertThat(amount).isEqualTo(new BigDecimal("1666.67"));
        }

        @Test
        @DisplayName("End before start is rejected")
        void endBeforeStartIsRejected() {
            assertThatThrownBy(() -> billingService.calculateAmount(
                    at("2024-03-15T10:00:00"), at("2024-03-15T09:00:00"), CAR))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    @DisplayName("Invoice generation")
    class InvoiceGeneration {

        @Test
        @DisplayName("Invoice carries billed minutes after grace and all amounts at scale 2")
        void generatesInvoiceForCompletedReservation() {
            Reservation reservation = completedCarReservation(at("2024-03-15T09:00:00"), at("2024-03-15T12:45:00"));

            ParkingInvoice invoice = billingService.generateInvoice(reservation, CAR);

            assertThat(invoice.getInvoiceId()).startsWith("INV-");
            assertThat(invoice.getReservationId()).isEqualTo(reservation.getReservationId());
            assertThat(invoice.getCustomerId()).isEqualTo("CUST-001");
            assertThat(invoice.getBilledDurationMinutes()).isEqualTo(210);
            assertThat(invoice.getBaseAmount()).isEqualTo(new BigDecimal("17500.00"));
            assertThat(invoice.getOvernightSurchargeApplied()).isEqualTo(new BigDecimal("0.00"));
            assertThat(invoice.getDiscountAmount()).isEqualTo(new BigDecimal("0.00"));
            assertThat(invoice.getTotalAmount()).isEqualTo(new BigDecimal("17500.00"));
            assertThat(invoice.getGeneratedAt()).isEqualTo(LocalDateTime.now(CLOCK));
        }

        @Test
        @DisplayName("Invoice separates base amount and overnight surcharge")
        void invoiceSplitsBaseAndOvernight() {
            Reservation reservation = completedCarReservation(at("2024-03-15T20:00:00"), at("2024-03-17T08:00:00"));

            ParkingInvoice invoice = billingService.generateInvoice(reservation, CAR);

            assertThat(invoice.getBaseAmount()).isEqualTo(new BigDecimal("110000.00"));
            assertThat(invoice.getOvernightSurchargeApplied()).isEqualTo(new BigDecimal("40000.00"));
            assertThat(invoice.getTotalAmount()).isEqualTo(new BigDecimal("150000.00"));
        }

        @Test
        @DisplayName("Invoice cannot be generated before check-out")
        void rejectsReservationThatIsNotCompleted() {
            Reservation pending = newCarReservation(at("2024-03-15T09:00:00"), at("2024-03-15T13:00:00"));

            assertThatThrownBy(() -> billingService.generateInvoice(pending, CAR))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("Rate card must match the reservation's vehicle type")
        void rejectsRateCardForAnotherVehicleType() {
            Reservation reservation = completedCarReservation(at("2024-03-15T09:00:00"), at("2024-03-15T10:00:00"));

            assertThatThrownBy(() -> billingService.generateInvoice(reservation, TRUCK))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        private Reservation newCarReservation(LocalDateTime start, LocalDateTime end) {
            var reservation = new Reservation();
            reservation.setReservationId("RES-TEST-1");
            reservation.setLotId("LOT-TEST01");
            reservation.setSlotId("A-01");
            reservation.setVehicleType(VehicleType.CAR);
            reservation.setLicensePlate("B 1234 XYZ");
            reservation.setCustomerId("CUST-001");
            reservation.setPlannedStartTime(start);
            reservation.setPlannedEndTime(end);
            reservation.setStatus(ReservationStatus.PENDING);
            reservation.setCreatedAt(start.minusDays(1));
            return reservation;
        }

        private Reservation completedCarReservation(LocalDateTime actualStart, LocalDateTime actualEnd) {
            var reservation = newCarReservation(actualStart, actualEnd);
            reservation.setActualStartTime(actualStart);
            reservation.setActualEndTime(actualEnd);
            reservation.setStatus(ReservationStatus.COMPLETED);
            return reservation;
        }
    }
}
