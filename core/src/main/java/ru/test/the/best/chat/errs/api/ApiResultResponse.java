package ru.test.the.best.chat.errs.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Единообразный wrapper для всех API ответов.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResultResponse<T> {

    private boolean success;
    private T data;
    private ApiError error;
    private LocalDateTime timestamp;

    public static <T> ApiResultResponse<T> success(T data) {
        return new ApiResultResponse<>(true, data, null, LocalDateTime.now());
    }

    public static <T> ApiResultResponse<T> failure(ApiError error) {
        return new ApiResultResponse<>(false, null, error, LocalDateTime.now());
    }

    public static <T> ApiResultResponse<T> failure(String code, String message) {
        return failure(new ApiError(code, message));
    }
}

