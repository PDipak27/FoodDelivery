package com.dpp.fd.delivery.service;

import com.dpp.fd.delivery.dto.AssignDeliveryRequest;
import com.dpp.fd.delivery.entity.Delivery;
import com.dpp.fd.delivery.entity.DeliveryAgent;
import com.dpp.fd.delivery.enums.DeliveryStatus;
import com.dpp.fd.delivery.exception.DeliveryException;
import com.dpp.fd.delivery.repository.DeliveryAgentRepository;
import com.dpp.fd.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryAgentRepository agentRepository;
    @InjectMocks private DeliveryService deliveryService;

    @Test
    void assign_freeAgentAvailable_returnsAssigned() {
        DeliveryAgent agent = DeliveryAgent.builder().id(UUID.randomUUID())
                .name("Rider A").isFree(true).userId(UUID.randomUUID()).build();
        AssignDeliveryRequest req = new AssignDeliveryRequest();
        req.setOrderId(UUID.randomUUID());

        when(agentRepository.findFirstByIsFreeTrue()).thenReturn(Optional.of(agent));
        when(agentRepository.save(any())).thenReturn(agent);
        when(deliveryRepository.save(any())).thenAnswer(inv -> {
            Delivery d = inv.getArgument(0); d.setId(UUID.randomUUID()); return d;
        });

        var response = deliveryService.assign(req);

        assertThat(response.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(agent.isFree()).isFalse();
    }

    @Test
    void assign_noAgentAvailable_throwsDeliveryException() {
        AssignDeliveryRequest req = new AssignDeliveryRequest();
        req.setOrderId(UUID.randomUUID());

        when(agentRepository.findFirstByIsFreeTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.assign(req))
                .isInstanceOf(DeliveryException.class)
                .hasMessageContaining("No available");
    }
}
