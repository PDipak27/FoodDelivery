package com.dpp.fd.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRestaurantRequest {
    @NotBlank private String name;
    @NotBlank private String cuisine;
    @NotBlank private String city;
}
