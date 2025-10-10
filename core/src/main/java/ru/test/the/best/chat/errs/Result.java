package ru.test.the.best.chat.errs;

import java.util.Objects;
import java.util.function.Function;

public class Result<T, E> {

    private final T value;
    private final E error;
    private final boolean isSuccess;

    private Result(T value, E error, boolean isSuccess) {
        this.value = value;
        this.error = error;
        this.isSuccess = isSuccess;
    }

    public static <T, E> Result<T, E> success(T value) {
        Objects.requireNonNull(value);
        return new Result<>(value, null, true);
    }

    // Упрощённая версия для Void-сценариев
    public static <E> Result<Void, E> success() {
        return new Result<>(null, null, true);
    }

    public static <T, E> Result<T, E> failure(E error) {
        Objects.requireNonNull(error);
        return new Result<>(null, error, false);
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public boolean isFailure() {
        return !isSuccess;
    }

    public T getValue() {
        if (!isSuccess)
            throw new IllegalStateException("Cannot get value from a failed result");
        return value;
    }

    public E getError() {
        if (isSuccess)
            throw new IllegalStateException("Cannot get error from a successful result");
        return error;
    }

    // Хелперы для обработки результата
    public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
        if (isSuccess)
            return Result.success(mapper.apply(value));
        return Result.failure(error);
    }

    public <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> mapper) {
        if (isSuccess)
            return mapper.apply(value);
        return Result.failure(error);
    }

    public Result<T, E> onFailure(Function<? super E, ?> handler) {
        if (!isSuccess) handler.apply(error);
        return this;
    }

    public Result<T, E> onSuccess(Function<? super T, ?> handler) {
        if (isSuccess) handler.apply(value);
        return this;
    }

    @Override
    public String toString() {
        return isSuccess
                ? String.format("Success(%s)", value)
                : String.format("Error(%s)", error);
    }
}
