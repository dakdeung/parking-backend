package com.demo.parking.service.seeder;

import com.demo.parking.constant.SlotStatus;
import com.demo.parking.constant.VehicleType;
import com.demo.parking.exception.InvalidRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParkingLotSeederServiceTest {

    private final ParkingLotSeederService parkingLotSeeder = new ParkingLotSeederService(null);

    @Test
    @DisplayName("New slots start AVAILABLE and are attached to their lot")
    void addSlotCreatesAvailableSlot() {
        var lot = parkingLotSeeder.newLot("LOT-JKT01", "Test", "Jakarta", LocalTime.MIDNIGHT, LocalTime.MIDNIGHT);

        var slot = parkingLotSeeder.addSlot(lot, "A-01", VehicleType.CAR, 0);

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
        assertThat(slot.getParkingLot()).isSameAs(lot);
        assertThat(lot.getSlots()).containsExactly(slot);
    }

    @Test
    @DisplayName("Lot id must match LOT-[A-Z0-9]{3,8}")
    void invalidLotIdIsRejected() {
        assertThatThrownBy(() -> parkingLotSeeder.newLot("PARK-1", "Test", "Jakarta",
                LocalTime.MIDNIGHT, LocalTime.MIDNIGHT))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Duplicate slot id and unknown vehicle type are rejected")
    void invalidSlotsAreRejected() {
        var lot = parkingLotSeeder.newLot("LOT-JKT01", "Test", "Jakarta", LocalTime.MIDNIGHT, LocalTime.MIDNIGHT);
        parkingLotSeeder.addSlot(lot, "A-01", VehicleType.CAR, 0);

        assertThatThrownBy(() -> parkingLotSeeder.addSlot(lot, "A-01", VehicleType.CAR, 0))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> parkingLotSeeder.addSlot(lot, "A-02", "BUS", 0))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Opening time must be before closing time (equal = 24h)")
    void invalidOperatingHoursAreRejected() {
        assertThatThrownBy(() -> parkingLotSeeder.newOperatingHours(LocalTime.of(22, 0), LocalTime.of(6, 0)))
                .isInstanceOf(InvalidRequestException.class);
    }
}
