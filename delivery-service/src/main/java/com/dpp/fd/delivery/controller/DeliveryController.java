package com.dpp.fd.delivery.controller;

import com.dpp.fd.delivery.dto.AssignDeliveryRequest;
import com.dpp.fd.delivery.dto.AssignDeliveryResponse;
import com.dpp.fd.delivery.dto.UpdateStatusRequest;
import com.dpp.fd.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("/assign")
    public AssignDeliveryResponse assign(@Valid @RequestBody AssignDeliveryRequest request) {
        return deliveryService.assign(request);
    }

    @PatchMapping("/{id}/status")
    public AssignDeliveryResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return deliveryService.updateStatus(id, request.getStatus());
    }
}
