package com.dpp.fd.restaurant.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MenuItemDto {
    private String itemId;
    private String name;
    private BigDecimal price;
    private boolean available;
}
