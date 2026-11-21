
package com.banking.system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

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
}
