package com.booking.gateway.filter;

import com.booking.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import java.nio.charset.StandardCharsets;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Jedino mesto u sistemu gde se JWT parsira.
 *
 * Odgovornosti:
 *  1. uklanja sva X-User-* zaglavlja koja je poslao klijent (u suprotnom bi svako mogao da se lažno predstavi kao korisnik),
 *  2. propušta javne putanje,
 *  3. validira Bearer token i prosleđuje identitet nizvodnom servisu kao pouzdana zaglavlja.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> SPOOFABLE_HEADERS = List.of("X-User-Id", "X-User-Email", "X-User-Role");

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtAuthenticationFilter(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Nikada ne veruj zaglavljima identiteta koja stižu spolja.
        ServerHttpRequest sanitized = request.mutate()
                .headers(headers -> SPOOFABLE_HEADERS.forEach(headers::remove))
                .build();

        String token = extractToken(sanitized);

        // 2. Javni endpoint-i: autentifikacija je opciona. Validan token se i dalje
        //    uvažava kako bi se identitet prosledio nizvodnom servisu - putanje kataloga
        //    su javne za GET/pregledanje, ali njihovi endpoint-i za upis (POST/PUT/DELETE/PATCH)
        //    oslanjaju se na @PreAuthorize i zato zahtevaju X-User-* zaglavlja. Nedostajući
        //    ili neispravan token na javnoj putanji jednostavno znači da se zahtev
        //    nastavlja anonimno.
        if (isPublic(path)) {
            if (token == null) {
                return chain.filter(exchange.mutate().request(sanitized).build());
            }
            try {
                ServerHttpRequest authenticated = withIdentity(sanitized, parse(token));
                return chain.filter(exchange.mutate().request(authenticated).build());
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Ignoring invalid token on public path {}: {}", path, ex.getMessage());
                return chain.filter(exchange.mutate().request(sanitized).build());
            }
        }

        // 3. Zaštićeni endpoint-i: validan Bearer token je obavezan.
        if (token == null) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Nedostaje Bearer token");
        }

        try {
            ServerHttpRequest authenticated = withIdentity(sanitized, parse(token));
            return chain.filter(exchange.mutate().request(authenticated).build());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected token for {}: {}", path, ex.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Token je neispravan ili je istekao");
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Prosleđuje verifikovani identitet nizvodnom servisu kao pouzdana zaglavlja. */
    private static ServerHttpRequest withIdentity(ServerHttpRequest request, Claims claims) {
        return request.mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Email", String.valueOf(claims.get("email")))
                .header("X-User-Role", String.valueOf(claims.get("role")))
                .build();
    }

    private boolean isPublic(String path) {
        return properties.publicPaths().stream().anyMatch(pattern -> MATCHER.match(pattern, path));
    }

    private static String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }

    private static Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}"
                .formatted(status.value(), status.getReasonPhrase(), message);
        // Eksplicitni UTF-8: getBytes() bez charset-a koristi podrazumevani platformski,
        // koji kvari dijakritike u srpskim porukama.
        var buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Izvršava se pre routing filtera.
        return -100;
    }
}
