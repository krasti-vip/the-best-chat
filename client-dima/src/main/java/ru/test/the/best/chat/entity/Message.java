package ru.test.the.best.chat.entity;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import ru.test.the.best.chat.entity.value.DataMessage;
import ru.test.the.best.chat.errs.Error;
import ru.test.the.best.chat.errs.GeneralErrors;
import ru.test.the.best.chat.errs.Guard;
import ru.test.the.best.chat.errs.Result;
import ru.test.the.best.chat.model.dto.message.CreateMessageRequest;
import ru.test.the.best.chat.model.dto.message.MessageResponse;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Value
@ToString
@EqualsAndHashCode
public class Message {

    UUID id;

    Instant date;

    UUID from;

    UUID to;

    DataMessage dataMessage;

    private Message(UUID id, Instant date, UUID from, UUID to, DataMessage dataMessage) {
        this.id = id;
        this.date = date;
        this.from = from;
        this.to = to;
        this.dataMessage = dataMessage;
    }

    private Message(Instant date, UUID from, UUID uuid, DataMessage dataMessage) {
        this(null, date, from, uuid, dataMessage);
    }

    public static Result<Message, Error> create(final UUID id, final Instant date, final UUID from, final UUID to, final DataMessage dataMessage) {
        if (Guard.isNull(date)) return Result.failure(GeneralErrors.valueIsEmpty("date"));
        if (Guard.isNullOrEmpty(from)) return Result.failure(GeneralErrors.valueIsEmpty("from"));
        if (Guard.isNullOrEmpty(to)) return Result.failure(GeneralErrors.valueIsEmpty("to"));
        if (Guard.isNull(dataMessage)) return Result.failure(GeneralErrors.valueIsEmpty("dataMessage"));

        return Result.success(new Message(id, date, from, to, dataMessage));
    }

    public static Result<Message, Error> update(final Instant date, final UUID from, final UUID to, final DataMessage dataMessage) {
        return create(UUID.randomUUID(), date, from, to, dataMessage);
    }

    public static Result<Message, Error> from(final CreateMessageRequest messageRequest) {
        if (Guard.isNullOrEmpty(messageRequest.type()))
            return Result.failure(GeneralErrors.valueIsEmpty("type field is empty or null"));
        if (Guard.isNull(messageRequest.date()))
            return Result.failure(GeneralErrors.valueIsEmpty("date is empty or null"));

        final var dateMessageErrorResult = DataMessage.create(messageRequest.getDataAsBytes(), messageRequest.type());

        if (dateMessageErrorResult.isFailure())
            return Result.failure(dateMessageErrorResult.getError());

        return update(messageRequest.date(), messageRequest.from(), messageRequest.to(), dateMessageErrorResult.getValue());
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
