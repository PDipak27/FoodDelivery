package com.dpp.fd.order.service;

import com.dpp.fd.order.client.*;
import com.dpp.fd.order.dto.*;
import com.dpp.fd.order.entity.Order;
import com.dpp.fd.order.entity.OrderItem;
import com.dpp.fd.order.enums.OrderStatus;
import com.dpp.fd.order.exception.OrderException;
import com.dpp.fd.order.exception.ResourceNotFoundException;
import com.dpp.fd.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Orchestrates the full order lifecycle synchronously.
 * Calls restaurant-service → payment-service → delivery-service in sequence;
 * notification calls are fire-and-forget (exceptions swallowed by NotificationClient).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantClient restaurantClient;
    private final PaymentClient paymentClient;
    private final NotificationClient notificationClient;
    private final DeliveryClient deliveryClient;

    @Transactional
    public OrderResponse placeOrder(UUID customerId, String customerEmail, PlaceOrderRequest req) {
        log.info("Placing order customerId={} restaurantId={} items={}",
                customerId, req.getRestaurantId(), req.getItems().size());

        // Validate restaurant and resolve item prices from restaurant-service
        RestaurantClient.RestaurantDto restaurant = restaurantClient.getRestaurant(req.getRestaurantId());
        if (!restaurant.isOpen()) {
            log.warn("Order rejected — restaurant {} is closed", req.getRestaurantId());
            throw new OrderException("Restaurant is currently closed");
        }

        Map<String, RestaurantClient.MenuItemDto> menuMap = new HashMap<>();
        for (RestaurantClient.MenuItemDto item : restaurant.menu()) {
            menuMap.put(item.itemId(), item);
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : req.getItems()) {
            RestaurantClient.MenuItemDto menuItem = menuMap.get(itemReq.getItemId());
            if (menuItem == null || !menuItem.available()) {
                log.warn("Order rejected — item not available itemId={} restaurantId={}",
                        itemReq.getItemId(), req.getRestaurantId());
                throw new OrderException("Item not available: " + itemReq.getItemId());
            }
            BigDecimal lineTotal = menuItem.price().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(lineTotal);
            items.add(OrderItem.builder()
                    .itemId(menuItem.itemId()).name(menuItem.name())
                    .quantity(itemReq.getQuantity()).unitPrice(menuItem.price())
                    .build());
        }

        log.info("Order total computed amount={} for customerId={}", total, customerId);

        // Charge payment; on failure save order with PAYMENT_FAILED status
        Order order = Order.builder()
                .customerId(customerId).restaurantId(req.getRestaurantId())
                .status(OrderStatus.PLACED).totalAmount(total).build();

        PaymentClient.ChargeResponse payment;
        try {
            payment = paymentClient.charge(order.getId() != null ? order.getId() : UUID.randomUUID(), total);
        } catch (OrderException ex) {
            log.warn("Payment call failed for customerId={} amount={} — marking PAYMENT_FAILED", customerId, total);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            return toResponse(order);
        }

        log.info("Payment result paymentId={} status={} for customerId={}", payment.paymentId(), payment.status(), customerId);

        order.setPaymentId(payment.paymentId());
        order.setStatus("SUCCESS".equals(payment.status()) ? OrderStatus.PLACED : OrderStatus.PAYMENT_FAILED);

        if (order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            log.warn("Payment declined paymentId={} amount={} customerId={}", payment.paymentId(), total, customerId);
        }

        // Link items to the order before saving
        final Order savedOrder = orderRepository.save(order);
        items.forEach(item -> item.setOrder(savedOrder));
        savedOrder.setItems(items);
        orderRepository.save(savedOrder);

        log.info("Order created orderId={} status={} customerId={}", savedOrder.getId(), savedOrder.getStatus(), customerId);

        notificationClient.send(customerEmail, "ORDER_PLACED",
                Map.of("orderId", savedOrder.getId().toString(), "total", total.toString()));

        return toResponse(savedOrder);
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus newStatus) {
        Order order = findOrThrow(orderId);
        OrderStatus previous = order.getStatus();
        validateTransition(previous, newStatus);
        order.setStatus(newStatus);
        log.info("Order {} status: {} → {}", orderId, previous, newStatus);

        if (newStatus == OrderStatus.ACCEPTED) {
            DeliveryClient.AssignResponse delivery = deliveryClient.assign(orderId);
            order.setDeliveryId(delivery.deliveryId());
            log.info("Delivery assigned deliveryId={} for orderId={}", delivery.deliveryId(), orderId);
        }

        orderRepository.save(order);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return toResponse(findOrThrow(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(UUID customerId) {
        return orderRepository.findByCustomerId(customerId).stream().map(this::toResponse).toList();
    }

    // --- helpers ---

    /**
     * Simple FSM guard — only allows meaningful forward transitions.
     * Prevents, e.g., jumping from DELIVERED back to PLACED.
     */
    private void validateTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PLACED         -> next == OrderStatus.ACCEPTED || next == OrderStatus.REJECTED || next == OrderStatus.CANCELLED;
            case ACCEPTED       -> next == OrderStatus.PREPARING;
            case PREPARING      -> next == OrderStatus.PICKED_UP;
            case PICKED_UP      -> next == OrderStatus.DELIVERED;
            default             -> false;
        };
        if (!valid) {
            throw new OrderException("Invalid status transition: " + current + " -> " + next);
        }
    }

    private Order findOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private OrderResponse toResponse(Order o) {
        List<OrderItemDto> itemDtos = o.getItems().stream()
                .map(i -> OrderItemDto.builder()
                        .itemId(i.getItemId()).name(i.getName())
                        .quantity(i.getQuantity()).unitPrice(i.getUnitPrice())
                        .lineTotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .toList();
        return OrderResponse.builder()
                .id(o.getId()).customerId(o.getCustomerId())
                .restaurantId(o.getRestaurantId()).status(o.getStatus())
                .totalAmount(o.getTotalAmount()).items(itemDtos).createdAt(o.getCreatedAt())
                .build();
    }
}
