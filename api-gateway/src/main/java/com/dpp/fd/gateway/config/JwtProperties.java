package com.dpp.fd.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

/**
 * Binds JWT configuration from application.yml.
 * Secret is injected via JWT_SECRET env variable — never hardcoded.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private long accessTokenExpiryMs;
}
