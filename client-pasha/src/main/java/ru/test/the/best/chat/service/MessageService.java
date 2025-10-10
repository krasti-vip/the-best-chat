package ru.test.the.best.chat.service;

import ru.test.the.best.chat.errs.*;
import ru.test.the.best.chat.errs.Error;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.test.the.best.chat.model.dto.message.CreateMessageRequest;
import ru.test.the.best.chat.model.dto.message.MessageResponse;
import ru.test.the.best.chat.model.entity.Message;
import ru.test.the.best.chat.model.value.DataMessage;
import ru.test.the.best.chat.repository.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для управления сообщениями.
 * Предоставляет CRUD операции и бизнес-логику для работы с сообщениями.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService implements ru.test.the.best.chat.service.Service<MessageResponse, UUID, CreateMessageRequest> {

    private final Repository<Message, UUID> messageRepository;

    /**
     * Получить все сообщения.
     *
     * @return список всех сообщений в системе
     */
    @Override
    public List<MessageResponse> findAll() {
        log.debug("Fetching all messages from database");

        try {
            final List<MessageResponse> messages = messageRepository.findAll()
                    .stream()
                    .map(Message::toMessageResponse)
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} messages", messages.size());
            return messages;
        } catch (Exception e) {
            log.error("Error occurred while fetching all messages", e);
            return Collections.emptyList();
        }
    }

    /**
     * Найти сообщение по ID.
     *
     * @param id уникальный идентификатор сообщения
     * @return Result с Optional<MessageResponse> или Error
     */
    @Override
    public Result<Optional<MessageResponse>, Error> findById(final UUID id) {
        log.debug("Attempting to find message by id: {}", id);

        if (Guard.isNullOrEmpty(id)) {
            log.warn("FindById called with null or empty id");
            return Result.failure(GeneralErrors.valueIsEmpty("id"));
        }

        try {
            final Result<Optional<Message>, Error> messageResult = messageRepository.findById(id);

            if (messageResult.isFailure()) {
                log.warn("Failed to find message with id: {}", id);
                return Result.failure(messageResult.getError());
            }

            final Optional<MessageResponse> response = messageResult.getValue()
                    .map(Message::toMessageResponse);

            if (response.isPresent()) {
                log.info("Message found with id: {}", id);
            } else {
                log.warn("Message not found with id: {}", id);
            }

            return Result.success(response);
        } catch (Exception e) {
            log.error("Error occurred while finding message by id: {}", id, e);
            return Result.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    /**
     * Удалить сообщение по ID.
     *
     * @param id уникальный идентификатор сообщения
     * @return UnitResult с результатом операции
     */
    @Override
    @Transactional
    public UnitResult<Error> deleteById(final UUID id) {
        log.debug("Attempting to delete message with id: {}", id);

        if (Guard.isNullOrEmpty(id)) {
            log.warn("DeleteById called with null or empty id");
            return UnitResult.failure(GeneralErrors.valueIsEmpty("id"));
        }

        try {
            final Result<Optional<Message>, Error> existingMessage = messageRepository.findById(id);

            if (existingMessage.isFailure()) {
                log.error("Failed to check message existence with id: {}", id);
                return UnitResult.failure(existingMessage.getError());
            }

            if (existingMessage.getValue().isEmpty()) {
                log.warn("Cannot delete: message not found with id: {}", id);
                return UnitResult.failure(GeneralErrors.entityNotFound("Message", id));
            }

            final UnitResult<Error> deleteResult = messageRepository.deleteById(id);

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

    /**
     * Обновить существующее сообщение.
     *
     * @param id                   уникальный идентификатор сообщения
     * @param createMessageRequest данные для обновления
     * @return UnitResult с результатом операции
     */
    @Override
    @Transactional
    public UnitResult<Error> update(final UUID id, final CreateMessageRequest createMessageRequest) {
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
            final Result<Optional<Message>, Error> existingMessageResult = messageRepository.findById(id);

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

            final Result<Message, Error> messageResult = Message.create(
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
            final UnitResult<Error> saveResult = messageRepository.save(messageToUpdate);

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

    /**
     * Создать новое сообщение.
     *
     * @param createMessageRequest данные нового сообщения
     * @return UnitResult с результатом операции
     */
    @Override
    @Transactional
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

            final UnitResult<Error> saveResult = messageRepository.save(newMessage);

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
            final List<MessageResponse> messages = messageRepository.findAllByFrom(fromUserId)
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
            final List<MessageResponse> messages = messageRepository.findAllByTo(toUserId)
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
            final List<MessageResponse> messages = messageRepository.findConversation(user1Id, user2Id)
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

