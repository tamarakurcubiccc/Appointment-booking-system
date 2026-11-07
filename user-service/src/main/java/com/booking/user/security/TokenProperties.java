package com.booking.user.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.tokens")
public record TokenProperties(Duration activationTtl, Duration passwordResetTtl) {
}
