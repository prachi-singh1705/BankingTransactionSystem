
package com.banking.system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.context.MessageSourceResolvable;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException exception) {

        String message = exception.getMessage();

        HttpStatus status = "Duplicate transaction ID".equals(message)
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;

        Map<String, String> response = Map.of(
                "status", "FAILED",
                "message", message
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(", "));

        Map<String, String> response = Map.of(
                "status", "FAILED",
                "message", message
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(
            ConstraintViolationException exception) {

        String message = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getMessage())
                .distinct()
                .collect(Collectors.joining(", "));

        Map<String, String> response = Map.of(
                "status", "FAILED",
                "message", message
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }


@ExceptionHandler(HandlerMethodValidationException.class)
public ResponseEntity<Map<String, String>> handleMethodValidation(
        HandlerMethodValidationException exception) {

    String message = exception.getParameterValidationResults()
            .stream()
            .flatMap(result -> result.getResolvableErrors().stream())
            .map(MessageSourceResolvable::getDefaultMessage)
            .filter(error -> error != null && !error.isBlank())
            .distinct()
            .collect(Collectors.joining(", "));

    if (message.isBlank()) {
        message = "Request validation failed";
    }

    Map<String, String> response = Map.of(
            "status", "FAILED",
            "message", message
    );

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
}

}
