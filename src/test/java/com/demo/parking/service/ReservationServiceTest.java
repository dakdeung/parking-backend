package com.demo.parking.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.demo.parking.constant.ReservationStatus;
import com.demo.parking.constant.SlotStatus;
import com.demo.parking.constant.VehicleType;
import com.demo.parking.entity.ParkingInvoice;
import com.demo.parking.exception.BusinessRuleViolationException;
import com.demo.parking.exception.InvalidRequestException;
import com.demo.parking.exception.ReservationConflictException;
import com.demo.parking.model.request.AvailabilityRequest;
import com.demo.parking.model.request.CancelReservationRequest;
import com.demo.parking.model.request.ReservationRequest;
import com.demo.parking.model.response.AvailableSlotResponse;
import com.demo.parking.repository.ParkingInvoiceRepository;
import com.demo.parking.repository.ParkingSlotRepository;
import com.demo.parking.repository.RateCardRepository;
import com.demo.parking.service.seeder.ParkingLotSeederService;
import com.demo.parking.service.seeder.RateCardSeederService;
import com.demo.parking.specification.ParkingInvoiceSpecification;
import com.demo.parking.specification.ParkingSlotSpecification;

/**
 * Reservation engine tests against a real (H2) database, so the overlap queries and row locks are exercised.
 */
@DataJpaTest
@Import({ReservationService.class, BillingService.class, ParkingLotSeederService.class, RateCardSeederService.class, InvoiceService.class, ReservationServiceTest.TestClockConfig.class})
class ReservationServiceTest {

    private static final String LOT_ID = "LOT-TEST01";
    private static final LocalDateTime NINE = LocalDateTime.parse("2024-03-15T09:00:00");
    private static final LocalDateTime THIRTEEN = LocalDateTime.parse("2024-03-15T13:00:00");

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private ParkingLotSeederService parkingLotSeederService;
    @Autowired
    private RateCardSeederService rateCardSeederService;
    @Autowired
    private BillingService billingService;
    @Autowired
    private ParkingSlotRepository slotRepository;
    @Autowired
    private RateCardRepository rateCardRepository;
    @Autowired
    private ParkingInvoiceRepository invoiceRepository;
    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.setTo(LocalDateTime.parse("2024-03-14T10:00:00"));
        var lot = parkingLotSeederService.newLot(LOT_ID, "Test Lot", "Jakarta", LocalTime.MIDNIGHT, LocalTime.MIDNIGHT);
        parkingLotSeederService.addSlot(lot, "B-01", VehicleType.CAR, 1);
        parkingLotSeederService.addSlot(lot, "A-02", VehicleType.CAR, 0);
        parkingLotSeederService.addSlot(lot, "A-01", VehicleType.CAR, 0);
        parkingLotSeederService.addSlot(lot, "T-01", VehicleType.TRUCK, 0);
        parkingLotSeederService.save(lot);
        rateCardRepository.saveAndFlush(rateCardSeederService.newRateCard(VehicleType.CAR,
                new BigDecimal("5000"), new BigDecimal("50000"), new BigDecimal("20000")));
    }

    private ReservationRequest carRequest(String customerId, LocalDateTime start, LocalDateTime end) {
        return new ReservationRequest(LOT_ID, VehicleType.CAR, "B 1234 XYZ", customerId, start, end, null);
    }

    private ReservationRequest truckRequest(String customerId, LocalDateTime start, LocalDateTime end) {
        return new ReservationRequest(LOT_ID, VehicleType.TRUCK, "B 9999 TRK", customerId, start, end, null);
    }

    private String slotStatus(String slotId) {
        var slotFilter = ParkingSlotSpecification.inLot(LOT_ID).and(ParkingSlotSpecification.hasSlotId(slotId));
        return slotRepository.findOne(slotFilter).orElseThrow().getStatus();
    }

    @Test
    @DisplayName("Creating a reservation returns PENDING and marks the slot RESERVED")
    void createReservationMarksSlotReserved() {
        var reservation = reservationService.createReservation(carRequest("CUST-001", NINE, THIRTEEN));

        assertThat(reservation.getReservationId()).startsWith("RES-");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getSlotId()).isEqualTo("A-01");
        assertThat(reservation.getFloorLevel()).isZero();
        assertThat(slotStatus("A-01")).isEqualTo(SlotStatus.RESERVED);
    }

    @Test
    @DisplayName("Double-booking the same slot for an overlapping window throws a conflict")
    void doubleBookingSameSlotIsRejected() {
        reservationService.createReservation(truckRequest("CUST-001", NINE, THIRTEEN));

        assertThatThrownBy(() -> reservationService.createReservation(
                truckRequest("CUST-002", NINE.plusHours(2), THIRTEEN.plusHours(2))))
                .isInstanceOf(ReservationConflictException.class);
    }

    @Test
    @DisplayName("Explicitly requesting an already booked slot throws a conflict")
    void explicitSlotDoubleBookingIsRejected() {
        reservationService.createReservation(carRequest("CUST-001", NINE, THIRTEEN));

        ReservationRequest sameSlot = new ReservationRequest(LOT_ID, VehicleType.CAR, "B 5678 ABC", "CUST-002",
                NINE.plusHours(1), THIRTEEN.plusHours(1), "A-01");

        assertThatThrownBy(() -> reservationService.createReservation(sameSlot))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("A-01");
    }

    @Test
    @DisplayName("Back-to-back reservations on the same slot do not overlap")
    void adjacentWindowsDoNotConflict() {
        reservationService.createReservation(truckRequest("CUST-001", NINE, THIRTEEN));

        var next = reservationService.createReservation(
                truckRequest("CUST-002", THIRTEEN, THIRTEEN.plusHours(2)));

        assertThat(next.getSlotId()).isEqualTo("T-01");
    }

    @Test
    @DisplayName("Check-in on time makes the reservation ACTIVE and the slot OCCUPIED")
    void checkInWithinWindowActivatesReservation() {
        var reservation = reservationService.createReservation(carRequest("CUST-001", NINE, THIRTEEN));

        var active = reservationService.checkIn(reservation.getReservationId(), NINE.plusMinutes(10));

        assertThat(active.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(active.getActualStartTime()).isEqualTo(NINE.plusMinutes(10));
        assertThat(slotStatus("A-01")).isEqualTo(SlotStatus.OCCUPIED);
    }

    @Test
    @DisplayName("Check-in more than 30 minutes late turns the reservation into NO_SHOW")
    void lateCheckInBecomesNoShow() {
        var reservation = reservationService.createReservation(carRequest("CUST-001", NINE, THIRTEEN));

        var result = reservationService.checkIn(reservation.getReservationId(), NINE.plusMinutes(31));

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
        assertThat(result.getActualStartTime()).isNull();
        assertThat(slotStatus("A-01")).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Check-in more than 30 minutes early is rejected")
    void earlyCheckInIsRejected() {
        var reservation = reservationService.createReservation(carRequest("CUST-001", NINE, THIRTEEN));

        assertThatThrownBy(() -> reservationService.checkIn(reservation.getReservationId(), NINE.minusMinutes(31)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("Cancelling a PENDING reservation returns the slot to AVAILABLE")
    void cancelPendingReservationFreesSlot() {
        var reservation = reservationService.createReservation(carRequest("CUST-001", NINE, THIRTEEN));

        var cancelled = reservationService.cancelReservation(reservation.getReservationId(), new CancelReservationRequest("change of plans"));

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled.getCancellationReason()).isEqualTo("change of plans");
        assertThat(cancelled.isLateCancellation()).isFalse();
        assertThat(slotStatus("A-01")).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Cancelling within 30 minutes of the start sets the late-cancellation flag")
    void cancelWithinThirtyMinutesIsLate() {
        var reservation = reservationService.createReservation(carRequest("CUST-001", NINE, THIRTEEN));
        clock.setTo(NINE.minusMinutes(20));

        var cancelled = reservationService.cancelReservation(reservation.getReservationId(), (CancelReservationRequest) null);

        assertThat(cancelled.isLateCancellation()).isTrue();
    }

    @Test
    @DisplayName("A completed reservation cannot be cancelled")
    void cancelCompletedReservationIsRejected() {
        var reservation = reservationService.createReservation(carRequest("CUST-001", NINE, THIRTEEN));
        reservationService.checkIn(reservation.getReservationId(), NINE);
        reservationService.checkOut(reservation.getReservationId(), NINE.plusHours(2));

        assertThatThrownBy(() -> reservationService.cancelReservation(reservation.getReservationId(), new CancelReservationRequest("late")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("Extending into a window booked by another customer throws a conflict")
    void extendIntoBookedWindowIsRejected() {
        var mine = reservationService.createReservation(truckRequest("CUST-001", NINE, THIRTEEN));
        reservationService.checkIn(mine.getReservationId(), NINE);
        reservationService.createReservation(truckRequest("CUST-002", THIRTEEN.plusHours(1), THIRTEEN.plusHours(3)));

        assertThatThrownBy(() -> reservationService.extendReservation(mine.getReservationId(), THIRTEEN.plusHours(2)))
                .isInstanceOf(ReservationConflictException.class);
    }

    @Test
    @DisplayName("Extending into a free window updates plannedEndTime")
    void extendIntoFreeWindowSucceeds() {
        var mine = reservationService.createReservation(truckRequest("CUST-001", NINE, THIRTEEN));
        reservationService.checkIn(mine.getReservationId(), NINE);

        var extended = reservationService.extendReservation(mine.getReservationId(), THIRTEEN.plusHours(2));

        assertThat(extended.getPlannedEndTime()).isEqualTo(THIRTEEN.plusHours(2));
    }

    @Test
    @DisplayName("Check-out completes the reservation, frees the slot and stores the invoice")
    void checkOutGeneratesInvoice() {
        var reservation = reservationService.createReservation(carRequest("CUST-001", NINE, THIRTEEN));
        reservationService.checkIn(reservation.getReservationId(), NINE);

        var completed = reservationService.checkOut(reservation.getReservationId(),
                LocalDateTime.parse("2024-03-15T12:45:00"));

        assertThat(completed.getReservation().getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(completed.getInvoice().getTotalAmount()).isEqualByComparingTo("17500.00");
        assertThat(slotStatus("A-01")).isEqualTo(SlotStatus.AVAILABLE);
        ParkingInvoice invoice = invoiceRepository.findOne(ParkingInvoiceSpecification.forReservation(reservation.getReservationId())).orElseThrow();
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("17500.00");
    }

    @Test
    @DisplayName("Availability uses the request filter; a missing filter is a 400")
    void availabilityUsesRequestFilter() {
        var truckFilter = new AvailabilityRequest(VehicleType.TRUCK, NINE, THIRTEEN);
        var carFilter = new AvailabilityRequest(VehicleType.CAR, NINE, THIRTEEN);

        assertThat(reservationService.findAvailableSlots(LOT_ID, truckFilter))
                .extracting(AvailableSlotResponse::getSlotId).containsExactly("T-01");
        assertThat(reservationService.findAvailableSlots(LOT_ID, carFilter))
                .extracting(AvailableSlotResponse::getSlotId).containsExactly("A-01", "A-02", "B-01");
        assertThatThrownBy(() -> reservationService.findAvailableSlots(LOT_ID, null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> reservationService.findAvailableSlots(LOT_ID, new AvailabilityRequest()))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Unknown vehicle type is rejected with a 400-style error")
    void unknownVehicleTypeIsRejected() {
        assertThatThrownBy(() -> reservationService.findAvailableSlots(LOT_ID,
                new AvailabilityRequest("BUS", NINE, THIRTEEN)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Limited-hours lot only accepts same-day windows inside opening hours")
    void operatingHoursAreEnforced() {
        var lot = parkingLotSeederService.newLot("LOT-BDG99", "Dago", "Bandung", LocalTime.of(6, 0), LocalTime.of(22, 0));
        parkingLotSeederService.addSlot(lot, "A-01", VehicleType.CAR, 0);
        parkingLotSeederService.save(lot);

        assertThat(reservationService.findAvailableSlots("LOT-BDG99",
                new AvailabilityRequest(VehicleType.CAR, NINE, THIRTEEN)))
                .extracting(AvailableSlotResponse::getSlotId).containsExactly("A-01");
        assertThatThrownBy(() -> reservationService.findAvailableSlots("LOT-BDG99",
                new AvailabilityRequest(VehicleType.CAR, NINE.withHour(5), THIRTEEN)))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> reservationService.findAvailableSlots("LOT-BDG99",
                new AvailabilityRequest(VehicleType.CAR, NINE.withHour(20), NINE.plusDays(1).withHour(7))))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    // ------------------------------------------------------------------ test clock

    @TestConfiguration
    static class TestClockConfig {

        @Bean
        MutableClock clock() {
            return new MutableClock();
        }
    }

    /** Clock whose "now" can be moved by a test. */
    static final class MutableClock extends Clock {

        private volatile Instant instant = Instant.EPOCH;

        void setTo(LocalDateTime dateTime) {
            instant = dateTime.toInstant(ZoneOffset.UTC);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
