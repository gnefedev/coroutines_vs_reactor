package com.gnefedev.coroutines.vs.reactor.util;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Component
public class TraceIdFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var traceId = Optional.ofNullable(exchange.getRequest().getHeaders().get("X-B3-TRACEID"))
                .orElse(Collections.emptyList())
                .stream().findAny().orElse(UUID.randomUUID().toString());
        return chain.filter(exchange)
                .contextWrite(context -> LoggerHelper.addEntryToMDCContext(context, "traceId", traceId));
    }
}
