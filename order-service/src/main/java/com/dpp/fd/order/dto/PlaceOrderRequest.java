package com.dpp.fd.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderRequest {
    @NotBlank  private String restaurantId;
    @NotEmpty  private List<OrderItemRequest> items;
}
