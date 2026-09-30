package com.demo.parking.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckOutResponse {
    private ReservationResponse reservation;
    private InvoiceResponse invoice;
}
