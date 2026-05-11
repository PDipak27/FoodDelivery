package com.dpp.fd.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores customer profile data. The id mirrors auth.users.id —
 * no foreign key across services; consistency enforced by convention.
 */
@Entity
@Table(name = "user_profiles", schema = "user_svc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfile {

    @Id
    private UUID id;   // Assigned from gateway header X-User-Id; no auto-generation

    @Column(nullable = false)
    private String name;

    private String phone;
    private String addressLine;
    private String city;
    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
