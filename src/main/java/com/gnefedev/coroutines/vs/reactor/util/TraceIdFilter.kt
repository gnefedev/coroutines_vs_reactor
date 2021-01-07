package com.gnefedev.coroutines.vs.reactor.util

import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.*

@Component
class TraceIdFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val traceId = Optional.ofNullable(exchange.request.headers["X-B3-TRACEID"])
                .orElse(emptyList())
                .stream().findAny().orElse(UUID.randomUUID().toString())
        MDC.put("traceId", traceId)
        return chain.filter(exchange)
    }
}