package com.demo.parking.model.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private String invoiceId;
    private String reservationId;
    private String customerId;
    private long billedDurationMinutes;
    @Schema(type = "string", example = "17500.00")
    private BigDecimal baseAmount;
    @Schema(type = "string", example = "0.00")
    private BigDecimal overnightSurchargeApplied;
    @Schema(type = "string", example = "0.00")
    private BigDecimal discountAmount;
    @Schema(type = "string", example = "17500.00")
    private BigDecimal totalAmount;
    @Schema(type = "string", example = "2024-03-15T12:45:00")
    private LocalDateTime generatedAt;
}
