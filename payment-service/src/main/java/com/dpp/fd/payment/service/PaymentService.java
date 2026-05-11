package com.dpp.fd.payment.service;

import com.dpp.fd.payment.dto.ChargeRequest;
import com.dpp.fd.payment.dto.ChargeResponse;
import com.dpp.fd.payment.dto.RefundResponse;
import com.dpp.fd.payment.entity.Payment;
import com.dpp.fd.payment.enums.PaymentStatus;
import com.dpp.fd.payment.exception.ResourceNotFoundException;
import com.dpp.fd.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock/stub payment service. Succeeds for all amounts ≤ 9999,
 * fails above that threshold — useful for simulating failure scenarios in tests.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final BigDecimal FAILURE_THRESHOLD = new BigDecimal("9999");

    private final PaymentRepository paymentRepository;

    @Transactional
    public ChargeResponse charge(ChargeRequest request) {
        PaymentStatus status = request.getAmount().compareTo(FAILURE_THRESHOLD) > 0
                ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;

        Payment payment = paymentRepository.save(Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .status(status)
                .build());

        return ChargeResponse.builder()
                .paymentId(payment.getId())
                .status(status)
                .message(status == PaymentStatus.SUCCESS ? "Payment successful" : "Payment declined")
                .build();
    }

    @Transactional
    public RefundResponse refund(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        return RefundResponse.builder().paymentId(paymentId).status(PaymentStatus.REFUNDED).build();
    }
}
