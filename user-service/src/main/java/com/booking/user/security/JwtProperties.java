package com.booking.user.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret       HMAC ključ kodiran u Base64, deljen sa API Gateway-om (env: JWT_SECRET).
 * @param issuer       Claim izdavača tokena.
 * @param expiration   Trajanje access tokena u sekundama.
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, String issuer, long expiration) {
}
