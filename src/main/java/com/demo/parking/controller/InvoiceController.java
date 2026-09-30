package com.demo.parking.controller;

import com.demo.parking.constant.UrlValues;
import com.demo.parking.model.response.InvoiceResponse;
import com.demo.parking.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UrlValues.INVOICES)
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping(UrlValues.RESERVATION_ID)
    @Operation(summary = "Invoice of a completed reservation")
    public InvoiceResponse get(
        @PathVariable String reservationId
    ) {
        return invoiceService.getByReservationId(
            reservationId
        );
    }
}
