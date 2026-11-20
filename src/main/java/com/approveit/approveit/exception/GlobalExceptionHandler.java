
package com.approveit.approveit.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(
            BusinessException ex) {

        String message = ex.getMessage();
        HttpStatus status;

        if ("User not found".equals(message)
                || "Payment not found".equals(message)) {
            status = HttpStatus.NOT_FOUND;

        } else if (message != null &&
                (message.contains("Only")
                        || message.contains("cannot")
                        || message.contains("already")
                        || message.contains("limit"))) {
            status = HttpStatus.FORBIDDEN;

        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        return ResponseEntity.status(status)
                .body(Map.of("message", message));
    }
}
