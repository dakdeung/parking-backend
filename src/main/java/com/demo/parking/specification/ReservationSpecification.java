package com.demo.parking.specification;

import com.demo.parking.entity.Reservation;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collection;

public final class ReservationSpecification {

    private ReservationSpecification() {
    }

    public static Specification<Reservation> inLot(String lotId) {
        return (root, query, builder) -> builder.equal(
            root.get("lotId"), lotId
        );
    }

    public static Specification<Reservation> onSlot(String slotId) {
        return (root, query, builder) -> builder.equal(
            root.get("slotId"), slotId
        );
    }

    public static Specification<Reservation> statusIn(Collection<String> statuses) {
        return (root, query, builder) -> {
            if (!statuses.isEmpty()) {
                return root.get("status").in(statuses);
            }
            return builder.and();
        };
    }

    /** Planned window overlaps the half-open window [start, end). */
    public static Specification<Reservation> overlaps(LocalDateTime start, LocalDateTime end) {
        return (root, query, builder) -> builder.and(
                builder.lessThan(root.get("plannedStartTime"), end),
                builder.greaterThan(root.get("plannedEndTime"), start));
    }

    public static Specification<Reservation> excluding(String reservationId) {
        return (root, query, builder) -> builder.notEqual(
            root.get("reservationId"), 
            reservationId
        );
    }
}
