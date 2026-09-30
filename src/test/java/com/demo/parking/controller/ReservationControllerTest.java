package com.demo.parking.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.demo.parking.configuration.JacksonConfiguration;
import com.demo.parking.configuration.SecurityConfiguration;
import com.demo.parking.constant.ReservationStatus;
import com.demo.parking.constant.SlotStatus;
import com.demo.parking.constant.VehicleType;
import com.demo.parking.exception.InvalidRequestException;
import com.demo.parking.model.request.CancelReservationRequest;
import com.demo.parking.model.response.AvailableSlotResponse;
import com.demo.parking.model.response.CheckOutResponse;
import com.demo.parking.model.response.InvoiceResponse;
import com.demo.parking.model.response.ReservationResponse;
import com.demo.parking.service.InvoiceService;
import com.demo.parking.service.ReservationService;

@WebMvcTest(controllers = {ReservationController.class, LotController.class, InvoiceController.class})
@Import({SecurityConfiguration.class, JacksonConfiguration.class})
class ReservationControllerTest {

    private static final String RESERVATION_ID = "RES-550e8400-e29b-41d4";
    private static final LocalDateTime NINE = LocalDateTime.parse("2024-03-15T09:00:00");
    private static final LocalDateTime THIRTEEN = LocalDateTime.parse("2024-03-15T13:00:00");
    private static final LocalDateTime CHECK_OUT = LocalDateTime.parse("2024-03-15T12:45:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private InvoiceService invoiceService;

    private static ReservationResponse reservation(
        String status, 
        LocalDateTime actualStart, 
        LocalDateTime actualEnd,
        boolean lateCancellation, 
        String cancellationReason
) {
        return new ReservationResponse(
                RESERVATION_ID, 
                "LOT-JKT01", 
                "A-01", 
                0, 
                VehicleType.CAR, 
                "B 1234 XYZ",
                "CUST-001", 
                status, 
                NINE, 
                THIRTEEN, 
                actualStart, 
                actualEnd, 
                lateCancellation, 
                cancellationReason
        );
    }


    private static InvoiceResponse invoice() {
        return new InvoiceResponse(
                "INV-1",
                 RESERVATION_ID, 
                 "CUST-001", 
                 210,
                 new BigDecimal("17500.00"), 
                 new BigDecimal("0.00"), 
                 new BigDecimal("0.00"),
                 new BigDecimal("17500.00"), 
                 CHECK_OUT
                );
    }

    
    @Test
    @DisplayName("DELETE /reservations/{id} returns the cancelled reservation with lateCancellation")
    void cancelReturnsLateCancellationFlag() throws Exception {
        var cancelled = reservation(
                ReservationStatus.CANCELLED, 
                null, 
                null, 
                true, 
                "change of plans"
        );
        when(reservationService.cancelReservation(eq(RESERVATION_ID), any(CancelReservationRequest.class)))
                .thenReturn(cancelled);

        mockMvc.perform(delete("/api/v1/reservations/" + RESERVATION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"change of plans\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.lateCancellation").value(true));
    }

    @Test
    @DisplayName("Check-out returns reservation + invoice with money serialised as strings")
    void checkOutReturnsInvoiceWithMoneyAsStrings() throws Exception {
        var completed = reservation(ReservationStatus.COMPLETED, NINE, CHECK_OUT, false, null);
        when(reservationService.checkOut(eq(RESERVATION_ID), any()))
                .thenReturn(new CheckOutResponse(completed, invoice()));

        mockMvc.perform(post("/api/v1/reservations/" + RESERVATION_ID + "/check-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualEndTime\":\"2024-03-15T12:45:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservation.status").value("COMPLETED"))
                .andExpect(jsonPath("$.invoice.totalAmount").value("17500.00"))
                .andExpect(jsonPath("$.invoice.baseAmount").value("17500.00"))
                .andExpect(jsonPath("$.invoice.billedDurationMinutes").value(210));
    }

    @Test
    @DisplayName("GET /invoices/{reservationId} returns the invoice")
    void getInvoiceReturnsInvoice() throws Exception {
        when(invoiceService.getByReservationId(RESERVATION_ID)).thenReturn(invoice());

        mockMvc.perform(get("/api/v1/invoices/" + RESERVATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value("INV-1"))
                .andExpect(jsonPath("$.totalAmount").value("17500.00"));
    }

    @Test
    @DisplayName("POST /lots/{lotId}/availability accepts the filter as a JSON request body")
    void availabilityWithRequestBody() throws Exception {
        when(reservationService.findAvailableSlots(eq("LOT-JKT01"),
                argThat(b -> b != null && VehicleType.CAR.equals(b.getVehicleType())
                        && NINE.equals(b.getStartTime()) && THIRTEEN.equals(b.getEndTime()))))
                .thenReturn(List.of(new AvailableSlotResponse("A-01", 0, VehicleType.CAR, SlotStatus.AVAILABLE)));

        mockMvc.perform(post("/api/v1/lots/LOT-JKT01/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "CAR",
                                  "startTime": "2024-03-15T09:00:00",
                                  "endTime": "2024-03-15T13:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotId").value("A-01"))
                .andExpect(jsonPath("$[0].floorLevel").value(0));
    }

    @Test
    @DisplayName("Unknown vehicle type returns 400 with the field name")
    void invalidVehicleTypeReturns400() throws Exception {
        when(reservationService.findAvailableSlots(eq("LOT-JKT01"), any()))
                .thenThrow(new InvalidRequestException("vehicleType", "vehicleType must be one of MOTORCYCLE, CAR, TRUCK"));

        mockMvc.perform(post("/api/v1/lots/LOT-JKT01/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleType": "BUS",
                                  "startTime": "2024-03-15T09:00:00",
                                  "endTime": "2024-03-15T13:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("vehicleType"));
    }
}
