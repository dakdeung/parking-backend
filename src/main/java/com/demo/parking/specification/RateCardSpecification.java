package com.demo.parking.specification;

import com.demo.parking.entity.RateCard;
import org.springframework.data.jpa.domain.Specification;


public final class RateCardSpecification {

    private RateCardSpecification() {
    }

    public static Specification<RateCard> forVehicleType(String vehicleType) {
        return (root, query, builder) -> builder.equal(
            root.get("vehicleType"), vehicleType
        );
    }
}
