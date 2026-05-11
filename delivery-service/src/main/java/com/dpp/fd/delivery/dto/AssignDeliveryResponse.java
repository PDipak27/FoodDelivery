package com.dpp.fd.delivery.dto;

import com.dpp.fd.delivery.enums.DeliveryStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data @Builder
public class AssignDeliveryResponse {
    private UUID deliveryId;
    private UUID agentId;
    private DeliveryStatus status;
}
