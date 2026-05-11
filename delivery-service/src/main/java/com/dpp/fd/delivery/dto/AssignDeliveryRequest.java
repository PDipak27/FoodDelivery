package com.dpp.fd.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignDeliveryRequest {
    @NotNull private UUID orderId;
}
