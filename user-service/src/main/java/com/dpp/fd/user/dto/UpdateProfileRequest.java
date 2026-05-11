package com.dpp.fd.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank private String name;
    private String phone;
    private String addressLine;
    private String city;
}
