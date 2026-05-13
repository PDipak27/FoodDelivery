package com.dpp.fd.auth.service;

import com.dpp.fd.auth.dto.AuthResponse;
import com.dpp.fd.auth.dto.LoginRequest;
import com.dpp.fd.auth.dto.RefreshRequest;
import com.dpp.fd.auth.dto.RegisterRequest;
import com.dpp.fd.auth.entity.RefreshToken;
import com.dpp.fd.auth.entity.User;
import com.dpp.fd.auth.exception.AuthException;
import com.dpp.fd.auth.repository.RefreshTokenRepository;
import com.dpp.fd.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Core auth operations: register, login, token refresh, logout.
 * Refresh token rotation is applied on every refresh — old token is
 * revoked and a new one issued, preventing reuse after theft.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.refresh-token-expiry-days:7}")
    private int refreshTokenExpiryDays;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user email={} role={}", request.getEmail(), request.getRole());
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected — email already exists: {}", request.getEmail());
            throw new AuthException("Email already registered");
        }
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        userRepository.save(user);
        log.info("User registered successfully id={} email={}", user.getId(), user.getEmail());
        return buildTokenPair(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email={}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed — no account for email={}", request.getEmail());
                    return new AuthException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed — wrong password for email={}", request.getEmail());
            throw new AuthException("Invalid email or password");
        }
        log.info("Login successful userId={} role={}", user.getId(), user.getRole());
        return buildTokenPair(user);
    }

    /**
     * Rotates the refresh token: revokes the old one, issues a fresh pair.
     * Throws if the token is not found, expired, or already revoked.
     */
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = sha256(request.getRefreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Refresh rejected — token revoked or expired for userId={}", stored.getUser().getId());
            throw new AuthException("Refresh token expired or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        log.info("Token rotated for userId={}", stored.getUser().getId());
        return buildTokenPair(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresentOrElse(
                rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                    log.info("Logout — token revoked for userId={}", rt.getUser().getId());
                },
                () -> log.warn("Logout — token not found (already revoked or invalid)")
        );
    }

    // --- private helpers ---

    private AuthResponse buildTokenPair(User user) {
        String rawRefresh = UUID.randomUUID().toString();
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(rawRefresh))
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .build();
        refreshTokenRepository.save(rt);

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(rawRefresh)
                .tokenType("Bearer")
                .expiresInMs(jwtService.getAccessTokenExpiryMs())
                .build();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
