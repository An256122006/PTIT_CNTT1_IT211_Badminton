package org.example.project.exception;

import org.example.project.common.reponse.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalHandlerException {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach((error) -> {

            errors.put(error.getField(), error.getDefaultMessage());
        });
        ApiResponse apiResponse = ApiResponse.builder().success(false).data(null).error(errors).message("FAILER").build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);

    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException e) {

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(
                        ApiResponse.builder()
                                .success(false)
                                .status(415)
                                .message("Content-Type phải là application/json")
                                .data(null)
                                .build()
                );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e) {

        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .success(false)
                        .status(400)
                        .message("Request body không được để trống")
                        .build()
        );
    }


    @ExceptionHandler(ValidAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<?>> handleEmailAlreadyExists(
            ValidAlreadyExistsException e) {

        Map<String, String> errors = new HashMap<>();
        String field = e.getField();
        if (field != null && !field.isBlank()) {
            errors.put(field, e.getMessage());
        } else {
            errors.put("error", e.getMessage());
        }

        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .success(false)
                        .status(400)
                        .error(errors)
                        .message(e.getMessage())
                        .build()
        );
    }
    @ExceptionHandler(TokenInValid.class)
    public ResponseEntity<ApiResponse<?>> handleTokenInValid(
            TokenInValid tokenInValid
    ){
        return ResponseEntity.status(401).body(
                ApiResponse.builder()
                        .success(false)
                        .status(401)
                        .message(tokenInValid.getMessage())
                        .build()
        );
    }
    @ExceptionHandler(HttpNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(
            HttpNotFoundException e) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ApiResponse.builder()
                                .success(false)
                                .status(404)
                                .message(e.getMessage())
                                .build()
                );
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e){

        e.printStackTrace();

        return ResponseEntity.status(500)
                .body(
                        ApiResponse.builder()
                                .success(false)
                                .status(500)
                                .message(e.getMessage())
                                .build()
                );
    }
}
