package com.dpp.fd.payment.dto;

import com.dpp.fd.payment.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data @Builder
public class ChargeResponse {
    private UUID paymentId;
    private PaymentStatus status;
    private String message;
}
