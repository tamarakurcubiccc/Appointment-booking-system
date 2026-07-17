package com.booking.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class GatewayConfig {

    /**
     * CORS je ograničen na eksplicitnu listu dozvoljenih origin-a. Odražavanje svakog
     * origin-a ("*") uz istovremeno dozvoljavanje kredencijala omogućava bilo kom sajtu
     * da u ime korisnika šalje pozive sa kredencijalima, pa se to namerno izbegava.
     * Podrazumevane origin-e za lokalni razvoj u drugim okruženjima možete promeniti
     * preko promenljive okruženja CORS_ALLOWED_ORIGINS (razdvojene zarezom) ili preko
     * svojstva security.cors.allowed-origins.
     */
    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${security.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080}")
            List<String> allowedOrigins) {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
