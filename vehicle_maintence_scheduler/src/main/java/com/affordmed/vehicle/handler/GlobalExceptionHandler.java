package com.affordmed.vehicle.handler;

import com.affordmed.middleware.LoggingMiddleware;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final LoggingMiddleware loggingMiddleware;

    public GlobalExceptionHandler(LoggingMiddleware loggingMiddleware) {
        this.loggingMiddleware = loggingMiddleware;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        loggingMiddleware.Log("backend", "error", "handler", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        loggingMiddleware.Log("backend", "error", "handler", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleExternal(ExternalServiceException exception, HttpServletRequest request) {
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
