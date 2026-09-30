package com.demo.parking.repository;

import com.demo.parking.entity.RateCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RateCardRepository extends JpaRepository<RateCard, String>, JpaSpecificationExecutor<RateCard> {
}
