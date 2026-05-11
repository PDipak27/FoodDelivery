package com.dpp.fd.order.controller;

import com.dpp.fd.order.dto.*;
import com.dpp.fd.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** X-User-Email is forwarded by the gateway alongside X-User-Id for notification routing. */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestHeader("X-User-Id") UUID customerId,
            @RequestHeader(value = "X-User-Email", defaultValue = "") String customerEmail,
            @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(customerId, customerEmail, request));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }

    @GetMapping("/my")
    public List<OrderResponse> myOrders(@RequestHeader("X-User-Id") UUID customerId) {
        return orderService.getOrdersByCustomer(customerId);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return orderService.updateStatus(id, request.getStatus());
    }
}
