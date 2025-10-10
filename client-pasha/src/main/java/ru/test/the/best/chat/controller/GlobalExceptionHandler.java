package ru.test.the.best.chat.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.test.the.best.chat.errs.api.ApiError;
import ru.test.the.best.chat.errs.api.ApiResultResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений для REST API.
 * Перехватывает все исключения и возвращает единообразные ответы.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка ошибок валидации (@Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResultResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {

        log.warn("Validation error occurred: {}", ex.getMessage());

        List<ApiError.ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiError.ValidationError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        ApiError apiError = new ApiError(
                "validation.error",
                "Request validation failed",
                validationErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResultResponse.failure(apiError));
    }

    /**
     * Обработка ошибок парсинга JSON.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResultResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {

        log.warn("Failed to parse request body: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResultResponse.failure(
                        "invalid.request.body",
                        "Failed to parse request body. Please check JSON format."
                ));
    }

    /**
     * Обработка ошибок несоответствия типов параметров.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResultResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());

        String message = String.format(
                "Invalid value for parameter '%s'. Expected type: %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResultResponse.failure("invalid.parameter.type", message));
    }

    /**
     * Обработка отсутствующих обязательных параметров.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResultResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException ex) {

        log.warn("Missing required parameter: {}", ex.getParameterName());

        String message = String.format(
                "Required parameter '%s' is missing",
                ex.getParameterName()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResultResponse.failure("missing.parameter", message));
    }

    /**
     * Обработка IllegalArgumentException.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResultResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.warn("Illegal argument: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResultResponse.failure("illegal.argument", ex.getMessage()));
    }

    /**
     * Обработка всех остальных исключений.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResultResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResultResponse.failure(
                        "internal.server.error",
                        "An unexpected error occurred. Please try again later."
                ));
    }
}

