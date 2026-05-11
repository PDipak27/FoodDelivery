package com.dpp.fd.order.client;

import com.dpp.fd.order.exception.OrderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentClient {

    private final RestClient paymentRestClient;

    public ChargeResponse charge(UUID orderId, BigDecimal amount) {
        try {
            return paymentRestClient.post()
                    .uri("/payments/charge")
                    .body(new ChargeRequest(orderId, amount))
                    .retrieve()
                    .body(ChargeResponse.class);
        } catch (RestClientException ex) {
            throw new OrderException("Payment service unavailable: " + ex.getMessage());
        }
    }

    public record ChargeRequest(UUID orderId, BigDecimal amount) {}
    public record ChargeResponse(UUID paymentId, String status) {}
}
