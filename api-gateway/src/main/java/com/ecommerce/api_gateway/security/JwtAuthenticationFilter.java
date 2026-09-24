package com.ecommerce.api_gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final List<String> publicRoutes;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, @Value("${gateway.security.public-routes}") String publicRoutesConfig) {
        this.jwtUtil = jwtUtil;
        this.publicRoutes = Arrays.stream(publicRoutesConfig.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublicRoute(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for path: {}", path);
            throw new JwtAuthenticationException("Missing or invalid Authorization token");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            logger.warn("Invalid JWT token for path: {}", path);
            throw new JwtAuthenticationException("Invalid JWT token");
        }

        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        // Explicitly remove potentially spoofed headers
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(httpHeaders -> {
                    httpHeaders.remove("X-User-Id");
                    httpHeaders.remove("X-User-Role");
                    httpHeaders.add("X-User-Id", userId);
                    httpHeaders.add("X-User-Role", role);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicRoute(String path) {
        return publicRoutes.stream().anyMatch(route -> {
            if (route.endsWith("/**")) {
                String base = route.substring(0, route.length() - 3);
                return path.startsWith(base);
            }
            return path.equals(route);
        });
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
