package com.booking.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, String issuer, List<String> publicPaths) {
}
