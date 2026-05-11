package com.dpp.fd.order.client;

import com.dpp.fd.order.exception.OrderException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Calls restaurant-service to validate menu items before order placement.
 * Throws OrderException on connectivity failure so callers see a clean error.
 */
@Component
@RequiredArgsConstructor
public class RestaurantClient {

    private final RestClient restaurantRestClient;

    public RestaurantDto getRestaurant(String restaurantId) {
        try {
            return restaurantRestClient.get()
                    .uri("/restaurants/{id}", restaurantId)
                    .retrieve()
                    .body(RestaurantDto.class);
        } catch (RestClientException ex) {
            throw new OrderException("Restaurant service unavailable: " + ex.getMessage());
        }
    }

    public record RestaurantDto(String id, boolean isOpen, List<MenuItemDto> menu) {}
    public record MenuItemDto(String itemId, String name, BigDecimal price, boolean available) {}
}
