package com.booking.common.security;

/**
 * Zaglavlja koja ubacuje API gateway nakon što je validirao JWT.
 * Nizvodni servisi nikada sami ne parsiraju token - veruju gatewayu
 * i dostupni su samo iznutra, iz privatne mreže kontejnera.
 */
public final class GatewayHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String USER_ROLE = "X-User-Role";

    private GatewayHeaders() {
    }
}
