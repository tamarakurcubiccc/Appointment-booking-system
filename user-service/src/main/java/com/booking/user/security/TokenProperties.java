package com.booking.user.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param activationTtl    Koliko dugo važi aktivacioni link.
 * @param passwordResetTtl Koliko dugo važi link za promenu lozinke - kratko, jer omogućava
 *                         preuzimanje naloga ako nekome dospe u ruke.
 */
@ConfigurationProperties(prefix = "security.tokens")
public record TokenProperties(Duration activationTtl, Duration passwordResetTtl) {
}
