package com.dpp.fd.delivery.repository;

import com.dpp.fd.delivery.entity.DeliveryAgent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, UUID> {
    Optional<DeliveryAgent> findFirstByIsFreeTrue();
}
