package com.dpp.fd.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persists a SHA-256 hash of the raw refresh token.
 * Storing the hash (not raw) protects against DB-level compromise.
 */
@Entity
@Table(name = "refresh_tokens", schema = "auth",
        indexes = @Index(columnList = "tokenHash"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean revoked = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
