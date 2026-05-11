package com.dpp.fd.order.client;

import com.dpp.fd.order.exception.OrderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryClient {

    private final RestClient deliveryRestClient;

    public AssignResponse assign(UUID orderId) {
        try {
            return deliveryRestClient.post()
                    .uri("/deliveries/assign")
                    .body(new AssignRequest(orderId))
                    .retrieve()
                    .body(AssignResponse.class);
        } catch (RestClientException ex) {
            throw new OrderException("Delivery service unavailable: " + ex.getMessage());
        }
    }

    public record AssignRequest(UUID orderId) {}
    public record AssignResponse(UUID deliveryId) {}
}
