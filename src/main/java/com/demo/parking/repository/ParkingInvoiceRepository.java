package com.demo.parking.repository;

import com.demo.parking.entity.ParkingInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ParkingInvoiceRepository extends JpaRepository<ParkingInvoice, String>, JpaSpecificationExecutor<ParkingInvoice> {
}
