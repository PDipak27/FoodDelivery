package com.dpp.fd.order.service;

import com.dpp.fd.order.client.*;
import com.dpp.fd.order.dto.OrderItemRequest;
import com.dpp.fd.order.dto.PlaceOrderRequest;
import com.dpp.fd.order.entity.Order;
import com.dpp.fd.order.enums.OrderStatus;
import com.dpp.fd.order.exception.OrderException;
import com.dpp.fd.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private RestaurantClient restaurantClient;
    @Mock private PaymentClient paymentClient;
    @Mock private NotificationClient notificationClient;
    @Mock private DeliveryClient deliveryClient;

    @InjectMocks private OrderService orderService;

    @Test
    void placeOrder_validItems_savedWithPlacedStatus() {
        UUID customerId = UUID.randomUUID();
        PlaceOrderRequest req = buildRequest("rest-1", "item-1", 2);

        when(restaurantClient.getRestaurant("rest-1")).thenReturn(
                new RestaurantClient.RestaurantDto("rest-1", true,
                        List.of(new RestaurantClient.MenuItemDto("item-1", "Burger", new BigDecimal("100"), true))));
        when(paymentClient.charge(any(), any())).thenReturn(
                new PaymentClient.ChargeResponse(UUID.randomUUID(), "SUCCESS"));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0); o.setId(UUID.randomUUID()); return o;
        });

        var response = orderService.placeOrder(customerId, "user@test.com", req);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("200");
    }

    @Test
    void placeOrder_paymentFails_savedWithPaymentFailedStatus() {
        UUID customerId = UUID.randomUUID();
        PlaceOrderRequest req = buildRequest("rest-1", "item-1", 1);

        when(restaurantClient.getRestaurant("rest-1")).thenReturn(
                new RestaurantClient.RestaurantDto("rest-1", true,
                        List.of(new RestaurantClient.MenuItemDto("item-1", "Burger", new BigDecimal("100"), true))));
        when(paymentClient.charge(any(), any())).thenThrow(new OrderException("Payment service unavailable"));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = orderService.placeOrder(customerId, "user@test.com", req);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    }

    @Test
    void updateStatus_invalidTransition_throwsOrderException() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).status(OrderStatus.DELIVERED)
                .customerId(UUID.randomUUID()).restaurantId("r1")
                .totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(orderId, OrderStatus.PLACED))
                .isInstanceOf(OrderException.class)
                .hasMessageContaining("Invalid status transition");
    }

    private PlaceOrderRequest buildRequest(String restaurantId, String itemId, int qty) {
        OrderItemRequest item = new OrderItemRequest();
        item.setItemId(itemId); item.setQuantity(qty);
        PlaceOrderRequest req = new PlaceOrderRequest();
        req.setRestaurantId(restaurantId); req.setItems(List.of(item));
        return req;
    }
}
