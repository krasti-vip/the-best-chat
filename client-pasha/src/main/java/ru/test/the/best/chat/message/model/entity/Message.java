package ru.test.the.best.chat.message.model.entity;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import ru.test.the.best.chat.errs.Error;
import ru.test.the.best.chat.errs.GeneralErrors;
import ru.test.the.best.chat.errs.Guard;
import ru.test.the.best.chat.errs.Result;
import ru.test.the.best.chat.model.dto.message.CreateMessageRequest;
import ru.test.the.best.chat.model.dto.message.MessageResponse;
import ru.test.the.best.chat.core.model.value.DataMessage;

import java.time.Instant;
import java.util.UUID;

@Value
@ToString
@EqualsAndHashCode(of = "id")
public class Message {

    UUID id;

    Instant date;

    UUID from;

    UUID to;

    DataMessage dataMessage;

    private Message(UUID id, Instant date, UUID from, UUID uuid, DataMessage dataMessage) {
        this.id = id;
        this.date = date;
        this.from = from;
        this.to = uuid;
        this.dataMessage = dataMessage;
    }

    private Message(Instant date, UUID from, UUID uuid, DataMessage dataMessage) {
        this(null, date, from, uuid, dataMessage);
    }

    public static Result<Message, Error> create(
            final Instant date,
            final UUID from,
            final UUID to,
            final DataMessage dataMessage,
            final UUID id
    ) {
        if (Guard.isNullOrEmpty(from)) return Result.failure(GeneralErrors.valueIsEmpty("from"));
        if (Guard.isNullOrEmpty(to)) return Result.failure(GeneralErrors.valueIsEmpty("to"));
        if (Guard.isNull(date)) return Result.failure(GeneralErrors.valueIsEmpty("date"));
        if (Guard.isNull(dataMessage)) return Result.failure(GeneralErrors.valueIsEmpty("dataMessage"));

        return Result.success(new Message(id, date, from, to, dataMessage));
    }

    public static Result<Message, Error> create(
            final Instant date,
            final UUID from,
            final UUID to,
            final DataMessage dataMessage
    ) {
        return create(date, from, to, dataMessage, UUID.randomUUID());
    }

    public static Result<Message, Error> from(final CreateMessageRequest messageRequest) {
        if (Guard.isNullOrEmpty(messageRequest.type()))
            return Result.failure(GeneralErrors.valueIsEmpty("type field is empty or null"));
        if (Guard.isNull(messageRequest.date()))
            return Result.failure(GeneralErrors.valueIsEmpty("date field is null"));


        final var dataMessageErrorResult = DataMessage.create(messageRequest.getDataAsBytes(), messageRequest.type());

        if (dataMessageErrorResult.isFailure()) return Result.failure(dataMessageErrorResult.getError());

        return create(
                messageRequest.date(),
                messageRequest.from(),
                messageRequest.to(),
                dataMessageErrorResult.getValue()
        );
    }

    public MessageResponse toMessageResponse() {
        return new MessageResponse(
                this.id,
                this.date,
                this.from,
                this.to,
                this.dataMessage.deserializeToString().getValue(),
                this.dataMessage.getType()
        );
    }
}
