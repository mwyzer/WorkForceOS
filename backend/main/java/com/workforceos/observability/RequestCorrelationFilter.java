package com.workforceos.observability;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestCorrelationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = CorrelationContext.randomHex(16);
        }
        CorrelationContext.start(requestId, request.getHeader("traceparent"));

        StatusCapturingResponse statusResponse = new StatusCapturingResponse(response);
        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, statusResponse);
        } finally {
            actorFrom(SecurityContextHolder.getContext().getAuthentication()).ifPresent(CorrelationContext::actor);
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            String traceId = CorrelationContext.traceId();
            String spanId = CorrelationContext.newSpanId();
            LOGGER.info(
                    "request method={} path={} query={} status={} durationMs={} actor={}",
                    request.getMethod(), request.getRequestURI(),
                    Optional.ofNullable(request.getQueryString()).orElse(""),
                    statusResponse.getStatus(), durationMs, CorrelationContext.actor());
            setHeaderIfPossible(response, "X-Request-Id", requestId);
            setHeaderIfPossible(response, "traceparent", "00-" + traceId + "-" + spanId + "-01");
            CorrelationContext.clear();
        }
    }

    private static void setHeaderIfPossible(HttpServletResponse response, String name, String value) {
        if (response.isCommitted()) {
            return;
        }
        response.setHeader(name, value);
    }

    private static Optional<String> actorFrom(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(authentication.getName());
    }

    private static final class StatusCapturingResponse extends HttpServletResponseWrapper {

        private int capturedStatus = HttpServletResponse.SC_OK;

        StatusCapturingResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            capturedStatus = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            capturedStatus = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            capturedStatus = sc;
            super.sendError(sc, msg);
        }

        @Override
        public int getStatus() {
            return capturedStatus;
        }
    }
}