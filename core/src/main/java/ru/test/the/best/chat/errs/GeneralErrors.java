package ru.test.the.best.chat.errs;

import java.util.UUID;

/**
 * Утилитный класс для создания стандартных ошибок приложения.
 * Предоставляет фабричные методы для различных типов ошибок.
 */
public final class GeneralErrors {

    private GeneralErrors() {
        // Utility class, no instantiation
    }

    // ==================== NOT FOUND ERRORS ====================

    /**
     * Ошибка "Запись не найдена" для Long ID.
     *
     * @param entityName название сущности (например, "User", "Chat")
     * @param id         идентификатор записи
     * @return объект Error
     */
    public static Error notFound(String entityName, Long id) {
        validateEntityName(entityName);
        String idPart = (id == null) ? "" : " with ID '" + id + "'";
        return Error.of(
                "record.not.found",
                String.format("%s not found%s", entityName, idPart)
        );
    }

    /**
     * Ошибка "Запись не найдена" для UUID.
     *
     * @param entityName название сущности
     * @param id         идентификатор записи
     * @return объект Error
     */
    public static Error notFound(String entityName, UUID id) {
        validateEntityName(entityName);
        String idPart = (id == null) ? "" : " with ID '" + id + "'";
        return Error.of(
                "record.not.found",
                String.format("%s not found%s", entityName, idPart)
        );
    }

    /**
     * Ошибка "Запись не найдена" для String (например, username).
     *
     * @param entityName название сущности
     * @param fieldName  название поля для поиска
     * @param fieldValue значение поля
     * @return объект Error
     */
    public static Error notFoundByField(String entityName, String fieldName, String fieldValue) {
        validateEntityName(entityName);
        if (isNullOrEmpty(fieldName)) {
            throw new IllegalArgumentException("Field name must not be null or empty");
        }
        return Error.of(
                "record.not.found",
                String.format("%s not found with %s '%s'", entityName, fieldName, fieldValue)
        );
    }

    // ==================== DUPLICATE / CONFLICT ERRORS ====================

    /**
     * Ошибка "Запись уже существует".
     *
     * @param entityName название сущности
     * @param fieldName  название поля с дубликатом
     * @param fieldValue значение поля
     * @return объект Error
     */
    public static Error duplicateEntity(String entityName, String fieldName, String fieldValue) {
        validateEntityName(entityName);
        if (isNullOrEmpty(fieldName)) {
            throw new IllegalArgumentException("Field name must not be null or empty");
        }
        return Error.of(
                "duplicate.entity",
                String.format("%s with %s '%s' already exists", entityName, fieldName, fieldValue)
        );
    }

    /**
     * Ошибка "Конфликт данных".
     *
     * @param message описание конфликта
     * @return объект Error
     */
    public static Error conflict(String message) {
        return Error.of("data.conflict", message);
    }

    // ==================== VALIDATION ERRORS ====================

    /**
     * Ошибка валидации поля.
     *
     * @param fieldName название поля
     * @param reason    причина ошибки
     * @return объект Error
     */
    public static Error validationError(String fieldName, String reason) {
        if (isNullOrEmpty(fieldName)) {
            throw new IllegalArgumentException("Field name must not be null or empty");
        }
        String message = (reason != null && !reason.isBlank())
                ? String.format("Validation failed for field '%s': %s", fieldName, reason)
                : String.format("Validation failed for field '%s'", fieldName);

        return Error.of("validation.error", message);
    }

    /**
     * Ошибка "Значение невалидно".
     *
     * @param fieldName название поля
     * @param reason    причина невалидности
     * @return объект Error
     */
    public static Error valueIsInvalid(String fieldName, String reason) {
        if (isNullOrEmpty(fieldName)) {
            throw new IllegalArgumentException("Field name must not be null or empty");
        }

        String message = (reason != null && !reason.isBlank())
                ? String.format("Value is invalid for '%s': %s", fieldName, reason)
                : String.format("Value is invalid for '%s'", fieldName);

        return Error.of("value.is.invalid", message);
    }

    /**
     * Ошибка "Значение пустое или null".
     *
     * @param fieldName название поля
     * @return объект Error
     */
    public static Error valueIsEmpty(String fieldName) {
        if (isNullOrEmpty(fieldName)) {
            throw new IllegalArgumentException("Field name must not be null or empty");
        }
        return Error.of(
                "value.is.empty",
                String.format("Value for '%s' must not be null or empty", fieldName)
        );
    }

    /**
     * Ошибка "Значение обязательно".
     *
     * @param fieldName название поля
     * @return объект Error
     */
    public static Error valueIsRequired(String fieldName) {
        if (isNullOrEmpty(fieldName)) {
            throw new IllegalArgumentException("Field name must not be null or empty");
        }
        return Error.of(
                "value.is.required",
                String.format("Value is required for '%s'", fieldName)
        );
    }

    /**
     * Ошибка "Неверная длина строки".
     *
     * @param fieldName     название поля
     * @param currentLength текущая длина
     * @param minLength     минимальная длина
     * @param maxLength     максимальная длина
     * @return объект Error
     */
    public static Error invalidLength(String fieldName, int currentLength, int minLength, int maxLength) {
        if (isNullOrEmpty(fieldName)) {
            throw new IllegalArgumentException("Field name must not be null or empty");
        }
        return Error.of(
                "invalid.string.length",
                String.format("Invalid length for '%s': current=%d, expected range=[%d, %d]",
                        fieldName, currentLength, minLength, maxLength)
        );
    }

    /**
     * Ошибка "Неверная длина строки" (упрощенная версия).
     *
     * @param fieldName название поля
     * @return объект Error
     */
    public static Error invalidLength(String fieldName) {
        if (isNullOrEmpty(fieldName)) {
            throw new IllegalArgumentException("Field name must not be null or empty");
        }
        return Error.of(
                "invalid.string.length",
                String.format("Invalid length for '%s'", fieldName)
        );
    }

    /**
     * Ошибка "Недопустимый формат".
     *
     * @param fieldName      название поля
     * @param expectedFormat ожидаемый формат
     * @return объект Error
     */
    public static Error invalidFormat(String fieldName, String expectedFormat) {
        if (isNullOrEmpty(fieldName)) {
            throw new IllegalArgumentException("Field name must not be null or empty");
        }
        String message = (expectedFormat != null && !expectedFormat.isBlank())
                ? String.format("Invalid format for '%s'. Expected format: %s", fieldName, expectedFormat)
                : String.format("Invalid format for '%s'", fieldName);

        return Error.of("invalid.format", message);
    }

    // ==================== COLLECTION ERRORS ====================

    /**
     * Ошибка "Коллекция слишком мала".
     *
     * @param minSize     минимальный размер
     * @param currentSize текущий размер
     * @return объект Error
     */
    public static Error collectionIsTooSmall(int minSize, int currentSize) {
        return Error.of(
                "collection.is.too.small",
                String.format("Collection must contain at least %d items, but contains %d",
                        minSize, currentSize)
        );
    }

    /**
     * Ошибка "Коллекция слишком велика".
     *
     * @param maxSize     максимальный размер
     * @param currentSize текущий размер
     * @return объект Error
     */
    public static Error collectionIsTooLarge(int maxSize, int currentSize) {
        return Error.of(
                "collection.is.too.large",
                String.format("Collection must contain at most %d items, but contains %d",
                        maxSize, currentSize)
        );
    }

    // ==================== RANGE ERRORS ====================

    /**
     * Ошибка "Значение вне допустимого диапазона".
     *
     * @param fieldName название поля
     * @param value     текущее значение
     * @param minValue  минимальное значение
     * @param maxValue  максимальное значение
     * @param <T>       тип значения
     * @return объект Error
     */
    public static <T extends Comparable<T>> Error valueIsOutOfRange(
            String fieldName, T value, T minValue, T maxValue) {
        if (isNullOrEmpty(fieldName)) {
            throw new IllegalArgumentException("Field name must not be null or empty");
        }
        return Error.of(
                "value.is.out.of.range",
                String.format("Value '%s' for '%s' is out of range [%s, %s]",
                        value, fieldName, minValue, maxValue)
        );
    }

    // ==================== STATE ERRORS ====================

    /**
     * Ошибка "Недопустимое состояние".
     *
     * @param message описание ошибки
     * @return объект Error
     */
    public static Error illegalState(String message) {
        return Error.of("illegal.state", "Illegal state: " + message);
    }

    // ==================== DATABASE ERRORS ====================

    /**
     * Ошибка базы данных.
     *
     * @param message описание ошибки
     * @return объект Error
     */
    public static Error databaseError(String message) {
        return Error.of(
                "database.error",
                String.format("Database operation failed: %s", message)
        );
    }

    /**
     * Ошибка нарушения ограничения БД.
     *
     * @param constraintName название ограничения
     * @return объект Error
     */
    public static Error constraintViolation(String constraintName) {
        return Error.of(
                "constraint.violation",
                String.format("Database constraint violated: %s", constraintName)
        );
    }

    // ==================== SERIALIZATION / DESERIALIZATION ====================

    /**
     * Ошибка десериализации.
     *
     * @param message описание ошибки
     * @return объект Error
     */
    public static Error deserializationError(String message) {
        return Error.of("deserialization.error", "Deserialization error: " + message);
    }

    /**
     * Ошибка сериализации.
     *
     * @param message описание ошибки
     * @return объект Error
     */
    public static Error serializationError(String message) {
        return Error.of("serialization.error", "Serialization error: " + message);
    }

    // ==================== SERVER ERRORS ====================

    /**
     * Внутренняя ошибка сервера.
     *
     * @param message описание ошибки
     * @return объект Error
     */
    public static Error internalServerError(String message) {
        return Error.of("internal.server.error", message);
    }

    /**
     * Сервис временно недоступен.
     *
     * @param serviceName название сервиса
     * @return объект Error
     */
    public static Error serviceUnavailable(String serviceName) {
        return Error.of(
                "service.unavailable",
                String.format("Service '%s' is temporarily unavailable", serviceName)
        );
    }

    // ==================== AUTHORIZATION / AUTHENTICATION ====================

    /**
     * Ошибка "Доступ запрещен".
     *
     * @param reason причина запрета
     * @return объект Error
     */
    public static Error accessDenied(String reason) {
        String message = (reason != null && !reason.isBlank())
                ? "Access denied: " + reason
                : "Access denied";
        return Error.of("access.denied", message);
    }

    /**
     * Ошибка аутентификации.
     *
     * @return объект Error
     */
    public static Error authenticationFailed() {
        return Error.of("authentication.failed", "Authentication failed");
    }

    /**
     * Ошибка "Токен недействителен".
     *
     * @return объект Error
     */
    public static Error invalidToken() {
        return Error.of("invalid.token", "Token is invalid or expired");
    }

    // ==================== BUSINESS LOGIC ERRORS ====================

    /**
     * Ошибка бизнес-логики.
     *
     * @param code    код ошибки
     * @param message описание ошибки
     * @return объект Error
     */
    public static Error businessLogicError(String code, String message) {
        if (isNullOrEmpty(code)) {
            throw new IllegalArgumentException("Error code must not be null or empty");
        }
        return Error.of(code, message);
    }

    /**
     * Операция не поддерживается.
     *
     * @param operation название операции
     * @return объект Error
     */
    public static Error operationNotSupported(String operation) {
        return Error.of(
                "operation.not.supported",
                String.format("Operation '%s' is not supported", operation)
        );
    }

    // ==================== HELPER METHODS ====================

    /**
     * Проверка на null или пустую строку.
     *
     * @param s проверяемая строка
     * @return true если null или пустая
     */
    private static boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Ошибка "Сущность не найдена" (универсальный метод).
     *
     * @param entityName название сущности
     * @param id         идентификатор (любого типа)
     * @return объект Error
     */
    public static Error entityNotFound(String entityName, Object id) {
        validateEntityName(entityName);
        String idPart = (id == null) ? "" : " with ID '" + id + "'";
        return Error.of(
                "entity.not.found",
                String.format("%s not found%s", entityName, idPart)
        );
    }


    /**
     * Валидация названия сущности.
     *
     * @param entityName название сущности
     * @throws IllegalArgumentException если название null или пустое
     */
    private static void validateEntityName(String entityName) {
        if (isNullOrEmpty(entityName)) {
            throw new IllegalArgumentException("Entity name must not be null or empty");
        }
    }
}


