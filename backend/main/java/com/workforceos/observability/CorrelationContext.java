package com.workforceos.observability;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.MDC;

public final class CorrelationContext {

    public static final String REQUEST_ID = "requestId";
    public static final String TRACE_ID = "traceId";
    public static final String ACTOR = "actor";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private CorrelationContext() {
    }

    public static void start(String requestId, String incomingTraceparent) {
        MDC.put(REQUEST_ID, requestId == null || requestId.isBlank() ? randomHex(16) : requestId.trim());
        parseTraceparent(incomingTraceparent)
                .ifPresentOrElse(traceId -> MDC.put(TRACE_ID, traceId),
                        () -> MDC.put(TRACE_ID, randomHex(16)));
        MDC.remove(ACTOR);
    }

    public static void actor(String name) {
        if (name != null && !name.isBlank()) {
            MDC.put(ACTOR, name);
        }
    }

    public static void clear() {
        MDC.clear();
    }

    public static String traceId() {
        return Optional.ofNullable(MDC.get(TRACE_ID)).orElse("-");
    }

    public static String requestId() {
        return Optional.ofNullable(MDC.get(REQUEST_ID)).orElse("-");
    }

    public static String actor() {
        return Optional.ofNullable(MDC.get(ACTOR)).orElse("");
    }

    public static String newSpanId() {
        return randomHex(8);
    }

    public static String randomHex(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        StringBuilder sb = new StringBuilder(value.length * 2);
        for (byte b : value) {
            sb.append(HEX[(b >>> 4) & 0x0F]).append(HEX[b & 0x0F]);
        }
        return sb.toString();
    }

    private static Optional<String> parseTraceparent(String traceparent) {
        if (traceparent == null || traceparent.isBlank()) {
            return Optional.empty();
        }
        String[] parts = traceparent.trim().split("-");
        if (parts.length < 3 || !"00".equals(parts[0].toLowerCase(Locale.ROOT))) {
            return Optional.empty();
        }
        if (parts[1].matches("[0-9a-fA-F]{32}")) {
            return Optional.of(parts[1].toLowerCase(Locale.ROOT));
        }
        return Optional.empty();
    }
}