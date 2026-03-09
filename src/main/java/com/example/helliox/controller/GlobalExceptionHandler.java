package com.example.helliox.controller;

import jakarta.validation.ConstraintViolationException;
import org.openapitools.model.ErrorResponse;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception handler for all REST controllers.
 * Catches exceptions thrown during request processing and maps them
 * to structured {@link ErrorResponse} payloads with appropriate HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles {@code @RequestBody} field-level validation failures ({@code @Valid}).
     * Returns 400 Bad Request with the first failing field and its constraint message.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return toObjectEntity(buildError(HttpStatus.BAD_REQUEST, message));
    }

    /**
     * Handles method-level constraint violations (Spring Framework 6+ AOP-based validation).
     * Triggered when {@code @Valid} is applied to method parameters via {@code @Validated}.
     * Returns 400 Bad Request with the first violation message.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        return toObjectEntity(buildError(HttpStatus.BAD_REQUEST, message));
    }

    /**
     * Handles malformed or unreadable JSON request bodies.
     * Returns 400 Bad Request.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return toObjectEntity(buildError(HttpStatus.BAD_REQUEST, "Malformed JSON request"));
    }

    /**
     * Handles requests with an unsupported {@code Content-Type}.
     * Returns 415 Unsupported Media Type.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return toObjectEntity(buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"));
    }

    /**
     * Handles Bean Validation {@link ConstraintViolationException} thrown outside
     * of standard {@code @RequestBody} binding (e.g. path/query parameter validation).
     * Returns 400 Bad Request with the first violation message.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .findFirst()
                .orElse("Constraint violation");
        return buildError(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Fallback handler for any unhandled exception.
     * Returns 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message) {
        return new ResponseEntity<>(new ErrorResponse(status.value(), message), status);
    }

    private ResponseEntity<Object> toObjectEntity(ResponseEntity<ErrorResponse> response) {
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
}