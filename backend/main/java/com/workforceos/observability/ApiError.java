package com.workforceos.observability;

public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        String requestId) {
}