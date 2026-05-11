package com.dpp.fd.restaurant.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UpdateMenuRequest {
    @NotEmpty
    private List<MenuItemDto> items;
}
