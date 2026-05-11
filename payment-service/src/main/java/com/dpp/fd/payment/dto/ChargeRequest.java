package com.dpp.fd.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ChargeRequest {
    @NotNull  private UUID orderId;
    @NotNull @Positive private BigDecimal amount;
}
