package com.dpp.fd.delivery.service;

import com.dpp.fd.delivery.dto.AssignDeliveryRequest;
import com.dpp.fd.delivery.dto.AssignDeliveryResponse;
import com.dpp.fd.delivery.entity.Delivery;
import com.dpp.fd.delivery.entity.DeliveryAgent;
import com.dpp.fd.delivery.enums.DeliveryStatus;
import com.dpp.fd.delivery.exception.DeliveryException;
import com.dpp.fd.delivery.repository.DeliveryAgentRepository;
import com.dpp.fd.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Assigns a free agent to a delivery using first-available strategy.
 * In a production system this would consider proximity or workload balancing.
 */
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAgentRepository agentRepository;

    @Transactional
    public AssignDeliveryResponse assign(AssignDeliveryRequest request) {
        DeliveryAgent agent = agentRepository.findFirstByIsFreeTrue()
                .orElseThrow(() -> new DeliveryException("No available delivery agents"));

        agent.setFree(false);
        agentRepository.save(agent);

        Delivery delivery = deliveryRepository.save(Delivery.builder()
                .orderId(request.getOrderId())
                .agent(agent)
                .status(DeliveryStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build());

        return AssignDeliveryResponse.builder()
                .deliveryId(delivery.getId())
                .agentId(agent.getId())
                .status(delivery.getStatus())
                .build();
    }

    @Transactional
    public AssignDeliveryResponse updateStatus(UUID deliveryId, DeliveryStatus newStatus) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new DeliveryException("Delivery not found: " + deliveryId));

        delivery.setStatus(newStatus);

        // Free agent when delivery completes
        if (newStatus == DeliveryStatus.DELIVERED) {
            delivery.getAgent().setFree(true);
            agentRepository.save(delivery.getAgent());
        }

        deliveryRepository.save(delivery);
        return AssignDeliveryResponse.builder()
                .deliveryId(delivery.getId())
                .agentId(delivery.getAgent().getId())
                .status(delivery.getStatus())
                .build();
    }
}
