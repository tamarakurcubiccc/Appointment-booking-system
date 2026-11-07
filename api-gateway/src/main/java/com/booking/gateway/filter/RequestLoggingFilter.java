package com.booking.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        var request = exchange.getRequest().mutate().header(CORRELATION_ID, correlationId).build();
        exchange.getResponse().getHeaders().add(CORRELATION_ID, correlationId);

        long started = System.currentTimeMillis();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String cid = correlationId;

        return chain.filter(exchange.mutate().request(request).build())
                .doOnSuccess(v -> log.info("[{}] {} {} -> {} ({} ms)", cid, method, path,
                        exchange.getResponse().getStatusCode(), System.currentTimeMillis() - started));
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
