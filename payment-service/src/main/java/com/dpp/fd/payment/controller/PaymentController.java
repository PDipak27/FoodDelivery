package com.dpp.fd.payment.controller;

import com.dpp.fd.payment.dto.ChargeRequest;
import com.dpp.fd.payment.dto.ChargeResponse;
import com.dpp.fd.payment.dto.RefundResponse;
import com.dpp.fd.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/charge")
    public ChargeResponse charge(@Valid @RequestBody ChargeRequest request) {
        return paymentService.charge(request);
    }

    @PostMapping("/refund/{paymentId}")
    public RefundResponse refund(@PathVariable UUID paymentId) {
        return paymentService.refund(paymentId);
    }
}
