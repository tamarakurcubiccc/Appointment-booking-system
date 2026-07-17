package com.booking.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Vraća se kada je circuit breaker otvoren, umesto da se pozivalac ostavi da visi.
 */
@RestController
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/fallback/{service}")
    public ResponseEntity<Map<String, Object>> fallback(@PathVariable String service) {
        // Koji je servis aktivirao breaker jeste naš problem, ne pozivaočev -
        // to ide u log, a ne u telo odgovora.
        log.warn("Circuit breaker open, serving fallback for {}", service);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 503,
                        "error", "Service Unavailable",
                        "message", "Servis trenutno nije dostupan, pokušajte ponovo za koji trenutak"));
    }

    /**
     * /api/internal/** je isključivo za komunikaciju servis-servis. Na sve što stigne
     * spolja odgovara se običnim 404 - ne potvrđujemo da endpoint postoji.
     */
    @RequestMapping("/blocked")
    public ResponseEntity<Map<String, Object>> blocked() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 404,
                        "error", "Not Found"));
    }
}
