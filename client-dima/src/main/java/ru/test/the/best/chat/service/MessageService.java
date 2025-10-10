package ru.test.the.best.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.test.the.best.chat.entity.Message;
import ru.test.the.best.chat.entity.value.DataMessage;
import ru.test.the.best.chat.errs.*;
import ru.test.the.best.chat.errs.Error;
import ru.test.the.best.chat.model.dto.message.CreateMessageRequest;
import ru.test.the.best.chat.model.dto.message.MessageResponse;
import ru.test.the.best.chat.repository.MessageRedisRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageService implements ServiceChat<MessageResponse, UUID, CreateMessageRequest> {

    private final MessageRedisRepository repository;

    @Override
    public List<MessageResponse> findAll() {
        log.info("start findAll");

        try {
            final List<MessageResponse> messages = repository.findAll().stream()
                    .map(Message::toMessageResponse)
                    .toList();

            log.info("end findAll");

            return messages;
        } catch (Exception e) {
            log.error("error findAll", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Result<Optional<MessageResponse>, Error> findById(final UUID id) {
        log.info("start findById");

        if (Guard.isNullOrEmpty(id)) {
            log.warn("id is null");
            return Result.failure(GeneralErrors.valueIsEmpty("id"));
        }

        try {
            final Result<Optional<Message>, Error> messagesResult = repository.findById(id);

            if (messagesResult.isFailure()) {
                log.warn("messagesResult is null");
                return Result.failure(messagesResult.getError());
            }

            final Optional<MessageResponse> message = messagesResult.getValue()
                    .map(Message::toMessageResponse);

            if (message.isPresent()) {
                log.info("end findById");
            } else {
                log.warn("Message not found with id: {}", id);
            }

            return Result.success(message);
        } catch (Exception e) {
            log.error("error findById", e);
            return Result.failure(GeneralErrors.databaseError(e.getMessage()));

        }
    }

    @Override
    public UnitResult<Error> deleteById(final UUID id) {
        log.debug("Attempting to delete message with id: {}", id);

        if (Guard.isNullOrEmpty(id)) {
            log.warn("DeleteById called with null or empty id");
            return UnitResult.failure(GeneralErrors.valueIsEmpty("id"));
        }

        try {
            final Result<Optional<Message>, Error> existingMessage = repository.findById(id);

            if (existingMessage.isFailure()) {
                log.error("Failed to check message existence with id: {}", id);
                return UnitResult.failure(existingMessage.getError());
            }

            if (existingMessage.getValue().isEmpty()) {
                log.warn("Cannot delete: message not found with id: {}", id);
                return UnitResult.failure(GeneralErrors.entityNotFound("Message", id));
            }

            final UnitResult<Error> deleteResult = repository.deleteById(id);

            if (deleteResult.isSuccess()) {
                log.info("Successfully deleted message with id: {}", id);
            } else {
                log.error("Failed to delete message with id: {}", id);
            }

            return deleteResult;
        } catch (Exception e) {
            log.error("Error occurred while deleting message with id: {}", id, e);
            return UnitResult.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    @Override
    public UnitResult<Error> updateById(final UUID id, final CreateMessageRequest createMessageRequest) {
        log.debug("Attempting to update message with id: {}", id);

        if (Guard.isNullOrEmpty(id)) {
            log.warn("Update called with null or empty id");
            return UnitResult.failure(GeneralErrors.valueIsEmpty("id"));
        }

        if (Guard.isNull(createMessageRequest)) {
            log.warn("Update called with null createMessageRequest for id: {}", id);
            return UnitResult.failure(GeneralErrors.valueIsRequired("createMessageRequest"));
        }

        try {
            final Result<Optional<Message>, Error> existingMessageResult = repository.findById(id);

            if (existingMessageResult.isFailure()) {
                log.error("Failed to find message with id: {} during update", id);
                return UnitResult.failure(existingMessageResult.getError());
            }

            if (existingMessageResult.getValue().isEmpty()) {
                log.warn("Cannot update: message not found with id: {}", id);
                return UnitResult.failure(GeneralErrors.entityNotFound("Message", id));
            }

            final Result<DataMessage, Error> dataMessageResult = DataMessage.create(
                    createMessageRequest.getDataAsBytes(),
                    createMessageRequest.type()
            );

            if (dataMessageResult.isFailure()) {
                log.warn("Failed to create DataMessage during update for id: {}", id);
                return UnitResult.failure(dataMessageResult.getError());
            }

            final Result<Message, Error> messageResult = Message.update(
                    createMessageRequest.date(),
                    createMessageRequest.from(),
                    createMessageRequest.to(),
                    dataMessageResult.getValue()
            );

            if (messageResult.isFailure()) {
                log.warn("Failed to create Message during update for id: {}", id);
                return UnitResult.failure(messageResult.getError());
            }

            // Устанавливаем ID для обновления существующей записи
            final Message messageToUpdate = messageResult.getValue();

            // Сохраняем обновленное сообщение
            final UnitResult<Error> saveResult = repository.save(messageToUpdate);

            if (saveResult.isSuccess()) {
                log.info("Successfully updated message with id: {}", id);
            } else {
                log.error("Failed to save updated message with id: {}", id);
            }

            return saveResult;
        } catch (Exception e) {
            log.error("Error occurred while updating message with id: {}", id, e);
            return UnitResult.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    @Override
    public UnitResult<Error> save(final CreateMessageRequest createMessageRequest) {
        log.debug("Attempting to create new message from: {} to: {}",
                createMessageRequest != null ? createMessageRequest.from() : "null",
                createMessageRequest != null ? createMessageRequest.to() : "null");

        if (Guard.isNull(createMessageRequest)) {
            log.warn("Save called with null createMessageRequest");
            return UnitResult.failure(GeneralErrors.valueIsRequired("createMessageRequest"));
        }

        try {
            // Создаем сущность Message из request
            final Result<Message, Error> entityMessageResult = Message.from(createMessageRequest);

            if (entityMessageResult.isFailure()) {
                log.warn("Failed to create Message entity from request");
                return UnitResult.failure(entityMessageResult.getError());
            }

            final Message newMessage = entityMessageResult.getValue();

            final UnitResult<Error> validationResult = validateMessage(newMessage);
            if (validationResult.isFailure()) {
                log.warn("Validation failed for message creation");
                return validationResult;
            }

            final UnitResult<Error> saveResult = repository.save(newMessage);

            if (saveResult.isSuccess()) {
                log.info("Successfully created new message with id: {} from: {} to: {}",
                        newMessage.getId(),
                        newMessage.getFrom(),
                        newMessage.getTo());
            } else {
                log.error("Failed to save new message");
            }

            return saveResult;
        } catch (Exception e) {
            log.error("Error occurred while creating new message", e);
            return UnitResult.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    /**
     * Получить все сообщения от конкретного пользователя.
     *
     * @param fromUserId ID отправителя
     * @return список сообщений
     */
    @Transactional(readOnly = true)
    public Result<List<MessageResponse>, Error> findAllByFrom(final UUID fromUserId) {
        log.debug("Fetching all messages from user: {}", fromUserId);

        if (Guard.isNullOrEmpty(fromUserId)) {
            log.warn("FindAllByFrom called with null or empty fromUserId");
            return Result.failure(GeneralErrors.valueIsEmpty("fromUserId"));
        }

        try {
            final List<MessageResponse> messages = repository.findAllByFrom(fromUserId)
                    .stream()
                    .map(Message::toMessageResponse)
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} messages from user: {}", messages.size(), fromUserId);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("Error occurred while fetching messages from user: {}", fromUserId, e);
            return Result.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    /**
     * Получить все сообщения для конкретного пользователя.
     *
     * @param toUserId ID получателя
     * @return список сообщений
     */
    @Transactional(readOnly = true)
    public Result<List<MessageResponse>, Error> findAllByTo(final UUID toUserId) {
        log.debug("Fetching all messages to user: {}", toUserId);

        if (Guard.isNullOrEmpty(toUserId)) {
            log.warn("FindAllByTo called with null or empty toUserId");
            return Result.failure(GeneralErrors.valueIsEmpty("toUserId"));
        }

        try {
            final List<MessageResponse> messages = repository.findAllByTo(toUserId)
                    .stream()
                    .map(Message::toMessageResponse)
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} messages to user: {}", messages.size(), toUserId);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("Error occurred while fetching messages to user: {}", toUserId, e);
            return Result.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    /**
     * Получить переписку между двумя пользователями.
     *
     * @param user1Id ID первого пользователя
     * @param user2Id ID второго пользователя
     * @return список сообщений
     */
    @Transactional(readOnly = true)
    public Result<List<MessageResponse>, Error> findConversation(final UUID user1Id, final UUID user2Id) {
        log.debug("Fetching conversation between users: {} and {}", user1Id, user2Id);

        if (Guard.isNullOrEmpty(user1Id)) {
            log.warn("FindConversation called with null or empty user1Id");
            return Result.failure(GeneralErrors.valueIsEmpty("user1Id"));
        }

        if (Guard.isNullOrEmpty(user2Id)) {
            log.warn("FindConversation called with null or empty user2Id");
            return Result.failure(GeneralErrors.valueIsEmpty("user2Id"));
        }

        try {
            final List<MessageResponse> messages = repository.findConversation(user1Id, user2Id)
                    .stream()
                    .map(Message::toMessageResponse)
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} messages in conversation between users: {} and {}",
                    messages.size(), user1Id, user2Id);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("Error occurred while fetching conversation between users: {} and {}",
                    user1Id, user2Id, e);
            return Result.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    /**
     * Валидация сообщения.
     *
     * @param message сообщение для валидации
     * @return UnitResult с результатом валидации
     */
    private UnitResult<Error> validateMessage(final Message message) {
        if (Guard.isNull(message)) {
            log.debug("Message is null");
            return UnitResult.failure(GeneralErrors.valueIsRequired("message"));
        }

        if (Guard.isNullOrEmpty(message.getFrom())) {
            log.debug("Message 'from' field is null or empty");
            return UnitResult.failure(GeneralErrors.valueIsEmpty("from"));
        }

        if (Guard.isNullOrEmpty(message.getTo())) {
            log.debug("Message 'to' field is null or empty");
            return UnitResult.failure(GeneralErrors.valueIsEmpty("to"));
        }

        if (message.getFrom().equals(message.getTo())) {
            log.debug("Message 'from' and 'to' are the same user: {}", message.getFrom());
            return UnitResult.failure(
                    GeneralErrors.validationError("message", "User cannot send message to themselves")
            );
        }

        if (Guard.isNull(message.getDate())) {
            log.debug("Message date is null");
            return UnitResult.failure(GeneralErrors.valueIsEmpty("date"));
        }

        if (Guard.isNull(message.getDataMessage().getData())) {
            log.debug("Message data is null");
            return UnitResult.failure(GeneralErrors.valueIsEmpty("data"));
        }

        return UnitResult.success();
    }
}
