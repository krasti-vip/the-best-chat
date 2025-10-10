package ru.test.the.best.chat.model.value;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import ru.test.the.best.chat.errs.Error;
import ru.test.the.best.chat.errs.GeneralErrors;
import ru.test.the.best.chat.errs.Guard;
import ru.test.the.best.chat.errs.Result;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Value
@ToString
@EqualsAndHashCode
public class DataMessage {

    byte[] data;
    Type type;

    private DataMessage(final byte[] data, final Type type) {
        this.data = data;
        this.type = type;
    }

    public static Result<DataMessage, Error> create(
            final byte[] data, final String type
    ) {
        if (Guard.isNullOrEmpty(type)) return Result.failure(GeneralErrors.valueIsEmpty("type message is null"));
        if (Guard.isNull(data) || data.length == 0)
            return Result.failure(GeneralErrors.valueIsEmpty("data is null or empty"));

        final var maybeType = toType(type);

        if (maybeType.isFailure()) return Result.failure(GeneralErrors.illegalState(type));

        return Result.success(new DataMessage(data, maybeType.getValue()));
    }

    private static Result<Type, Error> toType(final String typeStr) {
        if (Guard.isNullOrEmpty(typeStr)) return Result.failure(GeneralErrors.valueIsEmpty("typeStr"));

        for (Type value : Type.values()) {
            if (value.name().equalsIgnoreCase(typeStr)) return Result.success(value);
        }

        return Result.failure(GeneralErrors.illegalState(typeStr));
    }

    /**
     * Десериализует массив байт в строку.
     *
     * @return Result, содержащий String в случае успеха или Error, если тип сообщения не STRING.
     * @apiNote Используется стандартная и безопасная кодировка UTF-8.
     */
    public Result<String, Error> deserializeToString() {
        if (this.type != Type.STRING)
            return Result.failure(GeneralErrors.illegalState("Cannot deserialize to String, message type is " + this.type));

        // Декодирование в строку с использованием стандартной кодировки UTF-8.
        return Result.success(new String(this.data, StandardCharsets.UTF_8));
    }

    /**
     * Десериализует массив байт в объект изображения.
     *
     * @return Result, содержащий BufferedImage в случае успеха или Error при ошибке.
     * @apiNote Ошибки могут возникнуть, если данные повреждены, имеют неподдерживаемый формат,
     * или если тип сообщения не IMAGE.
     */
    public Result<BufferedImage, Error> deserializeToImage() {
        if (this.type != Type.IMAGE)
            return Result.failure(GeneralErrors.illegalState("Cannot deserialize to Image, message type is " + this.type));

        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(this.data)) {
            final var image = ImageIO.read(inputStream);

            if (Guard.isNull(image))
                return Result.failure(GeneralErrors.deserializationError("Failed to decode image, unsupported format or corrupted data."));

            return Result.success(image);
        } catch (final IOException e) {
            return Result.failure(GeneralErrors.deserializationError("Failed to deserialize image due to an IO error: " + e.getMessage()));
        }
    }

    /**
     * Десериализует массив байт в аудиопоток.
     *
     * @return Result, содержащий AudioInputStream в случае успеха или Error при ошибке.
     * @apiNote AudioInputStream позволяет в дальнейшем получить формат аудио, его длину и воспроизвести.
     * Ошибки возникают при поврежденных данных, неподдерживаемом аудиоформате или если тип сообщения не SOUND.
     */
    public Result<AudioInputStream, Error> deserializeToSound() {
        if (this.type != Type.SOUND)
            return Result.failure(GeneralErrors.illegalState("Cannot deserialize to Sound, message type is " + this.type));

        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(this.data)) {
            return Result.success(AudioSystem.getAudioInputStream(inputStream));
        } catch (final UnsupportedAudioFileException e) {
            return Result.failure(GeneralErrors.deserializationError("Failed to deserialize sound, unsupported audio format: " + e.getMessage()));
        } catch (final IOException e) {
            return Result.failure(GeneralErrors.deserializationError("Failed to deserialize sound due to an IO error: " + e.getMessage()));
        }
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    public String getType() {
        return type.name();
    }

    private enum Type {
        STRING, IMAGE, SOUND
    }
}

