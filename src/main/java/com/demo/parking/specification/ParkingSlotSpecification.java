package com.demo.parking.specification;

import org.springframework.data.jpa.domain.Specification;

import com.demo.parking.entity.ParkingSlot;

public final class ParkingSlotSpecification {

    private ParkingSlotSpecification() {
    }

    public static Specification<ParkingSlot> inLot(String lotId) {
        return (root, query, builder) -> builder.equal(
            root.get("parkingLot").get("lotId"), lotId
        );
    }

    public static Specification<ParkingSlot> hasSlotId(String slotId) {
        return (root, query, builder) -> builder.equal(
            root.get("slotId"), slotId
        );
    }

    public static Specification<ParkingSlot> hasVehicleType(String vehicleType) {
        return (root, query, builder) -> builder.equal(
            root.get("vehicleType"), vehicleType
        );
    }
}
