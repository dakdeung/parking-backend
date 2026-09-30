package com.demo.parking.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.demo.parking.exception.ParkingException;
import com.demo.parking.exception.ResourceNotFoundException;
import com.demo.parking.model.response.InvoiceResponse;
import com.demo.parking.repository.ParkingInvoiceRepository;
import com.demo.parking.specification.ParkingInvoiceSpecification;

@Service
public class InvoiceService {

    private final ParkingInvoiceRepository invoiceRepository;

    public InvoiceService(ParkingInvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public InvoiceResponse getByReservationId(String reservationId) {
        try {
            var invoice = invoiceRepository.findOne(
            Specification.allOf(
                ParkingInvoiceSpecification.forReservation(reservationId)
            )).orElseThrow(() -> new ResourceNotFoundException(
                    "Invoice for reservation", 
                    reservationId
                )
            );
            
        return new InvoiceResponse(
                invoice.getInvoiceId(),
                invoice.getReservationId(),
                invoice.getCustomerId(),
                invoice.getBilledDurationMinutes(),
                invoice.getBaseAmount(),
                invoice.getOvernightSurchargeApplied(),
                invoice.getDiscountAmount(),
                invoice.getTotalAmount(),
                invoice.getGeneratedAt()
            );
        } catch (ParkingException e) {
            throw e;
        }
    }

}
