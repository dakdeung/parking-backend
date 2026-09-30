package com.demo.parking.specification;

import org.springframework.data.jpa.domain.Specification;

import com.demo.parking.entity.ParkingInvoice;

public final class ParkingInvoiceSpecification {

    private ParkingInvoiceSpecification() {
    }

    public static Specification<ParkingInvoice> forReservation(String reservationId) {
        return (root, query, builder) -> builder.equal(
            root.get("reservationId"), reservationId
        );
    }
}
