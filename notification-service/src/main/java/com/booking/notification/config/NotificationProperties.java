package com.booking.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param mailEnabled          Kada je false, e-mail se ne predaje SMTP serveru nego se samo
 *                             loguje - za lokalni razvoj bez SMTP-a (env: MAIL_ENABLED).
 * @param fromAddress          Adresa pošiljaoca.
 * @param timeZone             Zona u kojoj se vremena termina prikazuju u tekstu obaveštenja.
 *                             U bazi su u UTC-u, korisnik ih čita u svojoj zoni.
 * @param activationBaseUrl    Osnova linka za aktivaciju naloga, bez tokena.
 * @param passwordResetBaseUrl Osnova linka za promenu lozinke, bez tokena.
 */
@ConfigurationProperties(prefix = "notifications")
public record NotificationProperties(
        boolean mailEnabled,
        String fromAddress,
        String timeZone,
        String activationBaseUrl,
        String passwordResetBaseUrl
) {
}
