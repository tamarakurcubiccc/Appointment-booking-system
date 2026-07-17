package com.booking.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @param secret     Base64-kodiran HMAC ključ - mora biti identičan ključu user-service-a.
 * @param issuer     Očekivani issuer claim (izdavalac).
 * @param publicPaths Ant obrasci koji zaobilaze autentifikaciju.
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, String issuer, List<String> publicPaths) {
}
