package com.dpp.fd.payment.service;

import com.dpp.fd.payment.dto.ChargeRequest;
import com.dpp.fd.payment.entity.Payment;
import com.dpp.fd.payment.enums.PaymentStatus;
import com.dpp.fd.payment.exception.ResourceNotFoundException;
import com.dpp.fd.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @InjectMocks private PaymentService paymentService;

    @Test
    void charge_normalAmount_returnsSuccess() {
        ChargeRequest req = new ChargeRequest();
        req.setOrderId(UUID.randomUUID()); req.setAmount(new BigDecimal("500"));

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0); p.setId(UUID.randomUUID()); return p;
        });

        var response = paymentService.charge(req);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void charge_amountOver9999_returnsFailed() {
        ChargeRequest req = new ChargeRequest();
        req.setOrderId(UUID.randomUUID()); req.setAmount(new BigDecimal("10000"));

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0); p.setId(UUID.randomUUID()); return p;
        });

        var response = paymentService.charge(req);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void refund_existingPayment_returnsRefunded() {
        UUID pid = UUID.randomUUID();
        Payment p = Payment.builder().id(pid).status(PaymentStatus.SUCCESS)
                .orderId(UUID.randomUUID()).amount(BigDecimal.TEN).build();
        when(paymentRepository.findById(pid)).thenReturn(Optional.of(p));
        when(paymentRepository.save(any())).thenReturn(p);

        var response = paymentService.refund(pid);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void refund_notFound_throwsException() {
        UUID pid = UUID.randomUUID();
        when(paymentRepository.findById(pid)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> paymentService.refund(pid))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
