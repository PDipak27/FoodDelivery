package com.dpp.fd.auth.service;

import com.dpp.fd.auth.dto.LoginRequest;
import com.dpp.fd.auth.dto.RefreshRequest;
import com.dpp.fd.auth.dto.RegisterRequest;
import com.dpp.fd.auth.entity.RefreshToken;
import com.dpp.fd.auth.entity.User;
import com.dpp.fd.auth.enums.Role;
import com.dpp.fd.auth.exception.AuthException;
import com.dpp.fd.auth.repository.RefreshTokenRepository;
import com.dpp.fd.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiryDays", 7);
    }

    @Test
    void register_newEmail_returnsTokenPair() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@test.com");
        req.setPassword("password123");
        req.setRole(Role.CUSTOMER);

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.getAccessTokenExpiryMs()).thenReturn(900_000L);

        var response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isNotBlank();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsAuthException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("dup@test.com");
        req.setPassword("password123");
        req.setRole(Role.CUSTOMER);

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void login_wrongPassword_throwsAuthException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("wrong");

        User user = User.builder().id(UUID.randomUUID()).email(req.getEmail())
                .password("hashed").role(Role.CUSTOMER).build();

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void refresh_revokedToken_throwsAuthException() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(UUID.randomUUID().toString());

        RefreshToken rt = RefreshToken.builder()
                .revoked(true)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("revoked");
    }
}
