package ru.test.the.best.chat.errs.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Структура ошибки в API ответе.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    private String code;
    private String message;
    private List<ValidationError> validationErrors;

    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Data
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String message;
    }
}

