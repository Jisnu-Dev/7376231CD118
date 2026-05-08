package com.affordmed.notification.handler;

import com.affordmed.middleware.LoggingMiddleware;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final LoggingMiddleware loggingMiddleware;

    public GlobalExceptionHandler(LoggingMiddleware loggingMiddleware) {
        this.loggingMiddleware = loggingMiddleware;
    }

    @ExceptionHandler(ExternalNotificationServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleExternal(ExternalNotificationServiceException exception, HttpServletRequest request) {
        loggingMiddleware.Log("backend", "error", "handler", exception.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception exception, HttpServletRequest request) {
        loggingMiddleware.Log("backend", "fatal", "handler", exception.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        ApiErrorResponse response = new ApiErrorResponse();
        response.timestamp = LocalDateTime.now();
        response.status = status.value();
        response.error = status.getReasonPhrase();
        response.message = message;
        response.path = path;
        return ResponseEntity.status(status).body(response);
    }
}
