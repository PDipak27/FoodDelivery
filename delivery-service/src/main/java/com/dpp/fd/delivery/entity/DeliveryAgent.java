package com.dpp.fd.delivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "delivery_agents", schema = "delivery")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    private boolean isFree = true;
}
