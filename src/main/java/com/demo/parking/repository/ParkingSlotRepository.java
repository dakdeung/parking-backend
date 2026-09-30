package com.demo.parking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.demo.parking.entity.ParkingSlot;

public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, String>, JpaSpecificationExecutor<ParkingSlot> {

    @Query(value = """
            SELECT * FROM parking_slot
            WHERE lot_id = :lotId AND vehicle_type = :vehicleType
            ORDER BY floor_level, slot_id
            FOR UPDATE
            """, nativeQuery = true)
    List<ParkingSlot> lockByLotAndVehicleType(
        @Param("lotId") String lotId,
        @Param("vehicleType") String vehicleType);

    @Query(value = """
            SELECT * FROM parking_slot
            WHERE lot_id = :lotId AND slot_id = :slotId
            FOR UPDATE
            """, nativeQuery = true)
    Optional<ParkingSlot> lockByLotAndSlotId(
        @Param("lotId") String lotId, 
        @Param("slotId") String slotId
);
}
