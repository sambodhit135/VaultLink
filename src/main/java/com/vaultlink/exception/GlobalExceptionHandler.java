package com.vaultlink.exception;

import com.vaultlink.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception caught: {}", ex.getMessage(), ex);
        ApiResponse response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessages = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
                
        log.warn("Validation failed: {}", errorMessages);
        
        ApiResponse response = ApiResponse.builder()
                .success(false)
                .message("Validation failed: " + errorMessages)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
