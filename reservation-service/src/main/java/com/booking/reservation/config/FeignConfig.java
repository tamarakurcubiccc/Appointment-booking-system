package com.booking.reservation.config;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    /**
     * Bez automatskih ponovnih pokušaja. Ovu konfiguraciju deli CatalogClient, čiji bookSlot POST
     * NIJE idempotentan: ako catalog označi termin kao BOOKED ali se odgovor izgubi (npr. read
     * timeout), ponovni pokušaj vidi 409 koji book() prosleđuje van svog kompenzacionog bloka,
     * ostavljajući termin zaostao u BOOKED bez rezervacije. Pozivaoci greške obrađuju
     * eksplicitno (kompenzaciono oslobađanje), pa bi slepi ponovni pokušaj naneo više štete nego koristi.
     */
    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;
    }
}
