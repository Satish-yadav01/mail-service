package com.satish.mailservice.exception;

import com.satish.mailservice.dto.EmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<EmailResponse> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        log.error("File size exceeds maximum limit", exc);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new EmailResponse(false, "File size exceeds maximum limit of 10MB", 0, 0, 0));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<EmailResponse> handleIllegalArgumentException(IllegalArgumentException exc) {
        log.error("Invalid argument", exc);
        return ResponseEntity.badRequest()
                .body(new EmailResponse(false, exc.getMessage(), 0, 0, 0));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException exc) {
        Map<String, String> errors = new HashMap<>();
        exc.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        log.error("Validation error", exc);
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoHandlerFoundException(NoHandlerFoundException exc) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Endpoint not found");
        error.put("path", exc.getRequestURL());
        log.error("No handler found for: {}", exc.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception exc) {
        log.error("Unexpected error occurred", exc);
        Map<String, String> error = new HashMap<>();
        error.put("error", "An unexpected error occurred");
        error.put("message", exc.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
