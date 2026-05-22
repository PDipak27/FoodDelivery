package com.dpp.fd.gateway.filter;

import com.dpp.fd.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global filter that validates the JWT on every request.
 * Public paths are bypassed. On success, X-User-Id and X-User-Role
 * headers are forwarded to downstream services so they don't re-validate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    /** Always public regardless of HTTP method. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/register", "/auth/login", "/auth/refresh",
            "/actuator/health"
    );

    /** Public for GET only — mutations (POST/PUT/PATCH) require a valid JWT. */
    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/restaurants"
    );

    private final JwtProperties jwtProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(exchange)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = parseToken(token);
            String userId = claims.getSubject();
            String role   = claims.get("role",  String.class);
            String email  = claims.get("email", String.class);

            // Forward user context as headers; downstream services read these
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.header("X-User-Id",    userId)
                                   .header("X-User-Role",  role)
                                   .header("X-User-Email", email != null ? email : ""))
                    .build();
            return chain.filter(mutated);

        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT validation failed for path {}: {}", path, ex.getMessage());
            return unauthorized(exchange);
        }
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private boolean isPublicPath(ServerWebExchange exchange) {
        String path   = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }
        // Restaurant browsing is public for GET only;
        // create / update-menu / toggle-open require a valid JWT
        if ("GET".equals(method) && PUBLIC_GET_PATHS.stream().anyMatch(path::startsWith)) {
            return true;
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100; // Run before routing filters
    }
}
