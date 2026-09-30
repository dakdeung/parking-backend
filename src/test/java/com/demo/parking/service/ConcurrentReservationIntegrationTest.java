package com.demo.parking.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.demo.parking.constant.VehicleType;
import com.demo.parking.exception.ReservationConflictException;
import com.demo.parking.model.request.ReservationRequest;
import com.demo.parking.repository.ParkingSlotRepository;
import com.demo.parking.specification.ParkingSlotSpecification;

@SpringBootTest
class ConcurrentReservationIntegrationTest {

    private static final String LOT_ID = "LOT-JKT01";

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ParkingSlotRepository slotRepository;

    @Test
    @DisplayName("Concurrent bookings never double-book a slot")
    void concurrentBookingsNeverDoubleBookASlot() throws Exception {
        var truckSlots = (int) slotRepository.count(
                ParkingSlotSpecification.inLot(LOT_ID).and(ParkingSlotSpecification.hasVehicleType(VehicleType.TRUCK)));
        int threads = 8;
        LocalDateTime start = LocalDateTime.parse("2031-01-10T08:00:00");
        LocalDateTime end = start.plusHours(4);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<Optional<String>>> results = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                ReservationRequest request = new ReservationRequest(LOT_ID, VehicleType.TRUCK,
                        "B " + (1000 + i) + " TRK", "CUST-C" + i, start, end, null);
                Callable<Optional<String>> booking = () -> {
                    startSignal.await();
                    try {
                        var reservation = reservationService.createReservation(request);
                        return Optional.of(reservation.getSlotId());
                    } catch (ReservationConflictException expected) {
                        return Optional.empty();
                    }
                };
                results.add(pool.submit(booking));
            }
            startSignal.countDown();

            List<String> bookedSlots = new ArrayList<>();
            for (Future<Optional<String>> result : results) {
                result.get(30, TimeUnit.SECONDS).ifPresent(bookedSlots::add);
            }

            assertThat(truckSlots).isPositive();
            assertThat(bookedSlots).hasSize(truckSlots);
            assertThat(bookedSlots).doesNotHaveDuplicates();
        } finally {
            pool.shutdownNow();
        }
    }
}
