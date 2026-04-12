package com.satish.mailservice.exception;

import com.satish.mailservice.dto.EmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<EmailResponse> handleGenericException(Exception exc) {
        log.error("Unexpected error occurred", exc);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new EmailResponse(false, "An unexpected error occurred: " + exc.getMessage(), 0, 0, 0));
    }
}
