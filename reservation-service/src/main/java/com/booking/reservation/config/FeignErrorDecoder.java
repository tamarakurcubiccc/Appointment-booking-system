package com.booking.reservation.config;

import com.booking.common.exception.ApiException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Prevodi nizvodne HTTP greške u ApiException, tako da se 409 iz catalog-service
 * prikaže klijentu kao 409 umesto neprozirnog 500.
 */
public class FeignErrorDecoder implements ErrorDecoder {

    private static final Logger log = LoggerFactory.getLogger(FeignErrorDecoder.class);
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.resolve(response.status());
        log.warn("Downstream call {} failed with status {}", methodKey, response.status());

        if (status == null) {
            return defaultDecoder.decode(methodKey, response);
        }

        return switch (status) {
            case NOT_FOUND -> ApiException.notFound("Traženi podatak nije pronađen (" + serviceOf(methodKey) + ")");
            case CONFLICT -> ApiException.conflict("Termin više nije slobodan");
            case BAD_REQUEST -> ApiException.badRequest("Neispravan zahtev (" + serviceOf(methodKey) + ")");
            case FORBIDDEN -> ApiException.forbidden("Radnja nije dozvoljena (" + serviceOf(methodKey) + ")");
            default -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    serviceOf(methodKey) + " trenutno nije dostupan");
        };
    }

    private static String serviceOf(String methodKey) {
        int idx = methodKey.indexOf('#');
        return idx > 0 ? methodKey.substring(0, idx) : methodKey;
    }
}
