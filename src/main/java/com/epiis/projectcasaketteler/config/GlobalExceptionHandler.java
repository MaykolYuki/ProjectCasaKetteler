package com.epiis.projectcasaketteler.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(
            MaxUploadSizeExceededException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "error");
        response.put("listMessage",
                java.util.List.of("Error: El archivo supera el límite de 10 MB"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}