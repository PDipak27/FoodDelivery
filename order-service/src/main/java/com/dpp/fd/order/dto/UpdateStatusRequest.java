package com.dpp.fd.order.dto;

import com.dpp.fd.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotNull private OrderStatus status;
}
