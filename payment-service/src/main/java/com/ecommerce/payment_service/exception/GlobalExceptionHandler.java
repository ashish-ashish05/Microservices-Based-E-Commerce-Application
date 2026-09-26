package com.ecommerce.payment_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePaymentNotFound(PaymentNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<Map<String, String>> handleDuplicatePayment(DuplicatePaymentException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ProviderUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleProviderUnavailable(ProviderUnavailableException ex) {
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(InvalidPaymentRequestException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPaymentRequest(InvalidPaymentRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        log.error("Unexpected error occurred: ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", status.getName());
        response.put("message", message);
        return new ResponseEntity<>(response, status);
    }
}
