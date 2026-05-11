package com.dpp.fd.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data @Builder
public class UserProfileDto {
    private UUID id;
    private String name;
    private String phone;
    private String addressLine;
    private String city;
}
