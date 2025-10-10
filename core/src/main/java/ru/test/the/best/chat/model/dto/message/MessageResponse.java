package ru.test.the.best.chat.model.dto.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * DTO для ответа с данными сообщения.
 */
@Schema(description = "Ответ с данными сообщения")
public record MessageResponse(

        @Schema(
                description = "Уникальный идентификатор сообщения",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "Дата и время отправки сообщения (UTC)",
                example = "2025-10-04T06:57:24.759699Z",
                type = "string",
                format = "date-time"
        )
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant date,

        @Schema(
                description = "UUID отправителя",
                example = "1dd67872-86e8-4019-9e85-4ec3c52ee008"
        )
        UUID from,

        @Schema(
                description = "UUID получателя",
                example = "02fe619a-84a6-41a3-b6aa-3c1ae79d8253"
        )
        UUID to,

        @Schema(
                description = "Содержимое сообщения (текст или Base64 для бинарных данных)",
                example = "Hello, World!"
        )
        String data,

        @Schema(
                description = "Тип сообщения",
                example = "STRING",
                allowableValues = {"STRING", "IMAGE", "SOUND"}
        )
        String type
) {

    /**
     * Проверка, является ли сообщение текстовым.
     */
    @JsonIgnore
    public boolean isTextMessage() {
        return "TEXT".equalsIgnoreCase(type);
    }

    /**
     * Проверка, является ли сообщение бинарным (файл/изображение).
     */
    @JsonIgnore
    public boolean isBinaryMessage() {
        return type != null && type.matches("(?i)IMAGE|VIDEO|AUDIO|FILE");
    }

    /**
     * Получить данные как массив байтов.
     * Для бинарных типов декодирует из Base64.
     */
    @JsonIgnore
    public byte[] getDataAsBytes() {
        if (data == null) {
            return new byte[0];
        }

        if (isBinaryMessage()) {
            try {
                return Base64.getDecoder().decode(data);
            } catch (IllegalArgumentException e) {
                // Если не Base64, возвращаем как обычный текст
                return data.getBytes(StandardCharsets.UTF_8);
            }
        }

        return data.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Получить данные как текст.
     * Для бинарных типов декодирует из Base64.
     */
    @JsonIgnore
    public String getDataAsText() {
        if (isTextMessage()) {
            return data;
        }

        byte[] bytes = getDataAsBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Получить размер данных в байтах.
     */
    @JsonIgnore
    public int getDataSize() {
        return getDataAsBytes().length;
    }
}

