package com.workforceos.observability;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, WebRequest request) {
        ApiError body = build(ex.getStatusCode().value(), ex.getStatusCode().toString(), ex.getReason(),
                requestPath(request));
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            WebRequest request) {
        String supported = String.join(", ", ex.getSupportedHttpMethods().stream()
                .map(Object::toString).toList());
        String message = supported.isBlank() ? "Method not allowed"
                : "Method not allowed. Supported: " + supported;
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(build(HttpStatus.METHOD_NOT_ALLOWED.value(),
                        HttpStatus.METHOD_NOT_ALLOWED.toString(), message, requestPath(request)));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleInvalidBody(Exception ex, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(build(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.toString(),
                        "Malformed or missing request fields", requestPath(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, WebRequest request) {
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class)
                .error("Unhandled exception traceId={} requestId={}",
                        CorrelationContext.traceId(), CorrelationContext.requestId(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                        "Internal error", requestPath(request)));
    }

    private static ApiError build(int status, String error, String message, String path) {
        return new ApiError(
                Instant.now().toString(),
                status,
                error,
                message == null ? error : message,
                path,
                CorrelationContext.traceId(),
                CorrelationContext.requestId());
    }

    private static String requestPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}