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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Assigns a free agent to a delivery using first-available strategy.
 * In a production system this would consider proximity or workload balancing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAgentRepository agentRepository;

    @Transactional
    public AssignDeliveryResponse assign(AssignDeliveryRequest request) {
        log.info("Assigning delivery agent for orderId={}", request.getOrderId());

        DeliveryAgent agent = agentRepository.findFirstByIsFreeTrue()
                .orElseThrow(() -> {
                    log.warn("No free delivery agents available for orderId={}", request.getOrderId());
                    return new DeliveryException("No available delivery agents");
                });

        agent.setFree(false);
        agentRepository.save(agent);

        Delivery delivery = deliveryRepository.save(Delivery.builder()
                .orderId(request.getOrderId())
                .agent(agent)
                .status(DeliveryStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build());

        log.info("Delivery assigned deliveryId={} agentId={} agentName='{}' orderId={}",
                delivery.getId(), agent.getId(), agent.getName(), request.getOrderId());

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

        DeliveryStatus previous = delivery.getStatus();
        delivery.setStatus(newStatus);
        log.info("Delivery {} status: {} → {}", deliveryId, previous, newStatus);

        if (newStatus == DeliveryStatus.DELIVERED) {
            delivery.getAgent().setFree(true);
            agentRepository.save(delivery.getAgent());
            log.info("Agent {} '{}' is now free after completing delivery {}",
                    delivery.getAgent().getId(), delivery.getAgent().getName(), deliveryId);
        }

        deliveryRepository.save(delivery);
        return AssignDeliveryResponse.builder()
                .deliveryId(delivery.getId())
                .agentId(delivery.getAgent().getId())
                .status(delivery.getStatus())
                .build();
    }
}
