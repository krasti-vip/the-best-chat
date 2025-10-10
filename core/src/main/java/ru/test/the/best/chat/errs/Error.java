package ru.test.the.best.chat.errs;

import java.util.Objects;

public final class Error {

    private static final String SEPARATOR = "||";

    private final String code;
    private final String message;

    private Error(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static Error of(String code, String message) {
        return new Error(code, message);
    }

    public static Error deserialize(String serialized) {
        if ("A non-empty request body is required.".equals(serialized)) {
            return GeneralErrors.valueIsRequired("serialized");
        }

        String[] parts = serialized.split("\\|\\|");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid error serialization: '" + serialized + "'");
        }

        return new Error(parts[0], parts[1]);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String serialize() {
        return code + SEPARATOR + message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Error error)) return false;
        return Objects.equals(code, error.code) &&
               Objects.equals(message, error.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message);
    }

    @Override
    public String toString() {
        return "Error{" +
               "code='" + code + '\'' +
               ", message='" + message + '\'' +
               '}';
    }
}

