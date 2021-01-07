package com.gnefedev.coroutines.vs.reactor.util;

import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class LoggerHelper {
    private static final String MDC_ID_KEY = "MDC_context";

    public static Context addEntryToMDCContext(Context context, String key, String value) {
        Map<String, String> MDCContext = Objects.requireNonNull(context.getOrDefault(MDC_ID_KEY, new HashMap<>()));
        MDCContext.put(key, value);
        return context.put(MDC_ID_KEY, MDCContext);
    }


    public static <T, R> Function<T, Mono<R>> withMDC(Function<T, Mono<R>> block) {
        return value -> Mono.deferContextual(context -> {
            Optional<Map<String, String>> mdcContext = context.getOrEmpty(MDC_ID_KEY);
            if (mdcContext.isPresent()) {
                try {
                    MDC.setContextMap(mdcContext.get());
                    return block.apply(value);
                } finally {
                    MDC.clear();
                }
            } else {
                return block.apply(value);
            }
        });
    }
}
