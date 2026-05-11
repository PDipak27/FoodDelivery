package com.dpp.fd.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderItemRequest {
    @NotBlank private String itemId;
    @Min(1)  private int quantity;
}
