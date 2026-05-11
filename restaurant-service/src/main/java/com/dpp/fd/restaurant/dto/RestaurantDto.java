package com.dpp.fd.restaurant.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RestaurantDto {
    private String id;
    private String name;
    private String cuisine;
    private String city;
    private boolean isOpen;
    private List<MenuItemDto> menu;
}
