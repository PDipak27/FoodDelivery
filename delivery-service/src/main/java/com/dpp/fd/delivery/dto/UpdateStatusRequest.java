package com.dpp.fd.delivery.dto;

import com.dpp.fd.delivery.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotNull private DeliveryStatus status;
}
