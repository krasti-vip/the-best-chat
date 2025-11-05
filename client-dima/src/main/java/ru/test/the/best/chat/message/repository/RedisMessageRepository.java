package ru.test.the.best.chat.message.repository;

import ru.test.the.best.chat.errs.*;
import ru.test.the.best.chat.errs.Error;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.*;
import ru.test.the.best.chat.message.model.entity.Message;

import java.util.*;

/**
 * Репозиторий для работы с сообщениями в Redis.
 * Использует индексацию по отправителю и получателю для быстрого поиска.
 * <p>
 * Структура ключей:
 * - message:{id} - основное хранилище сообщений
 * - user:messages:from:{userId} - Set с ID сообщений от пользователя
 * - user:messages:to:{userId} - Set с ID сообщений для пользователя
 * - messages:all - Set со всеми ID сообщений
 */
@Slf4j
@Repository
public class RedisMessageRepository implements ru.test.the.best.chat.core.repository.Repository<Message, UUID> {

    // Префиксы ключей
    private static final String MESSAGE_KEY_PREFIX = "message:";
    private static final String USER_FROM_INDEX_PREFIX = "user:messages:from:";
    private static final String USER_TO_INDEX_PREFIX = "user:messages:to:";
    private static final String ALL_MESSAGES_KEY = "messages:all";

    private static final int MESSAGE_TTL = 86400 * 30; // 30 дней

    private final JedisPool jedisPool;
    private final Gson gson;

    @Autowired
    public RedisMessageRepository(final JedisPool jedisPool, final Gson gson) {
        this.jedisPool = Objects.requireNonNull(jedisPool, "JedisPool cannot be null");
        this.gson = Objects.requireNonNull(gson, "Gson cannot be null");
        log.info("RedisMessageRepository initialized with JedisPool and Gson");
    }

    /**
     * Сохранить сообщение с индексацией.
     *
     * @param message сообщение для сохранения
     * @return UnitResult с результатом операции
     */
    @Override
    public UnitResult<Error> save(final Message message) {
        log.debug("Attempting to save message with id: {}", message != null ? message.getId() : "null");

        if (Guard.isNull(message)) {
            log.warn("Save called with null message");
            return UnitResult.failure(GeneralErrors.valueIsRequired("message"));
        }

        if (Guard.isNullOrEmpty(message.getId())) {
            log.warn("Save called with message without ID");
            return UnitResult.failure(GeneralErrors.valueIsRequired("message.id"));
        }

        try (Jedis jedis = jedisPool.getResource()) {
            final String messageKey = MESSAGE_KEY_PREFIX + message.getId();
            final String fromIndexKey = USER_FROM_INDEX_PREFIX + message.getFrom();
            final String toIndexKey = USER_TO_INDEX_PREFIX + message.getTo();
            final String messageIdStr = message.getId().toString();

            // Сериализация
            final String jsonMessage = gson.toJson(message);

            Transaction transaction = jedis.multi();
            transaction.set(messageKey, jsonMessage);
            transaction.sadd(fromIndexKey, messageIdStr);
            transaction.sadd(toIndexKey, messageIdStr);
            transaction.sadd(ALL_MESSAGES_KEY, messageIdStr);
            transaction.expire(messageKey, MESSAGE_TTL);

            final var results = transaction.exec();

            if (results == null || results.isEmpty()) {
                log.error("Transaction failed while saving message: {}", message.getId());
                return UnitResult.failure(GeneralErrors.databaseError("Redis transaction failed"));
            }

            log.info("Successfully saved message: {} from {} to {}",
                    message.getId(), message.getFrom(), message.getTo());
            return UnitResult.success();

        } catch (Exception e) {
            log.error("Error occurred while saving message: {}", message.getId(), e);
            return UnitResult.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    /**
     * Найти сообщение по ID.
     *
     * @param id идентификатор сообщения
     * @return Result с Optional<Message> или Error
     */
    @Override
    public Result<Optional<Message>, Error> findById(final UUID id) {
        log.debug("Attempting to find message by id: {}", id);

        if (Guard.isNullOrEmpty(id)) {
            log.warn("FindById called with null or empty id");
            return Result.failure(GeneralErrors.valueIsEmpty("id"));
        }

        try (Jedis jedis = jedisPool.getResource()) {
            final String key = MESSAGE_KEY_PREFIX + id;
            final String jsonMessage = jedis.get(key);

            if (jsonMessage == null || jsonMessage.isEmpty()) {
                log.debug("Message not found with id: {}", id);
                return Result.success(Optional.empty());
            }

            final Message message = gson.fromJson(jsonMessage, Message.class);
            log.debug("Message found with id: {}", id);
            return Result.success(Optional.of(message));

        } catch (Exception e) {
            log.error("Error occurred while finding message by id: {}", id, e);
            return Result.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    /**
     * Найти все сообщения.
     * ВНИМАНИЕ: Может быть медленным при большом количестве сообщений!
     *
     * @return список всех сообщений
     */
    @Override
    public List<Message> findAll() {
        log.debug("Fetching all messages from Redis");

        try (Jedis jedis = jedisPool.getResource()) {
            final var allMessageIds = jedis.smembers(ALL_MESSAGES_KEY);

            if (allMessageIds.isEmpty()) {
                log.debug("No messages found in Redis");
                return Collections.emptyList();
            }

            log.debug("Found {} message IDs, fetching messages", allMessageIds.size());

            final var messages = getMessagesByIds(jedis, allMessageIds);

            log.info("Successfully fetched {} messages", messages.size());
            return messages;

        } catch (Exception e) {
            log.error("Error occurred while fetching all messages", e);
            return Collections.emptyList();
        }
    }

    /**
     * Удалить сообщение по ID.
     * Удаляет из основного хранилища и всех индексов.
     *
     * @param id идентификатор сообщения
     * @return UnitResult с результатом операции
     */
    @Override
    public UnitResult<Error> deleteById(final UUID id) {
        log.debug("Attempting to delete message with id: {}", id);

        if (Guard.isNullOrEmpty(id)) {
            log.warn("DeleteById called with null or empty id");
            return UnitResult.failure(GeneralErrors.valueIsEmpty("id"));
        }

        try (Jedis jedis = jedisPool.getResource()) {
            final var messageKey = MESSAGE_KEY_PREFIX + id;
            final var jsonMessage = jedis.get(messageKey);

            if (jsonMessage == null || jsonMessage.isEmpty()) {
                log.warn("Cannot delete: message not found with id: {}", id);
                return UnitResult.failure(GeneralErrors.entityNotFound("Message", id));
            }

            final var message = gson.fromJson(jsonMessage, Message.class);
            final var messageIdStr = id.toString();
            final var fromIndexKey = USER_FROM_INDEX_PREFIX + message.getFrom();
            final var toIndexKey = USER_TO_INDEX_PREFIX + message.getTo();

            Transaction transaction = jedis.multi();
            transaction.del(messageKey);
            transaction.srem(fromIndexKey, messageIdStr);
            transaction.srem(toIndexKey, messageIdStr);
            transaction.srem(ALL_MESSAGES_KEY, messageIdStr);

            final var results = transaction.exec();

            if (results == null || results.isEmpty()) {
                log.error("Transaction failed while deleting message: {}", id);
                return UnitResult.failure(GeneralErrors.databaseError("Redis transaction failed"));
            }

            log.info("Successfully deleted message: {} from {} to {}",
                    id, message.getFrom(), message.getTo());
            return UnitResult.success();

        } catch (Exception e) {
            log.error("Error occurred while deleting message with id: {}", id, e);
            return UnitResult.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    /**
     * Найти все сообщения от конкретного пользователя.
     * Использует индекс для быстрого поиска.
     *
     * @param fromUserId ID отправителя
     * @return список сообщений
     */
    @Override
    public List<Message> findAllByFrom(final UUID fromUserId) {
        log.debug("Fetching all messages from user: {}", fromUserId);

        if (Guard.isNullOrEmpty(fromUserId)) {
            log.warn("FindAllByFrom called with null or empty fromUserId");
            return Collections.emptyList();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            final var fromIndexKey = USER_FROM_INDEX_PREFIX + fromUserId;
            final var messageIds = jedis.smembers(fromIndexKey);

            if (messageIds.isEmpty()) {
                log.debug("No messages found from user: {}", fromUserId);
                return Collections.emptyList();
            }

            log.debug("Found {} message IDs from user: {}, fetching messages",
                    messageIds.size(), fromUserId);

            // Получаем сообщения через pipeline
            final var messages = getMessagesByIds(jedis, messageIds);

            log.info("Successfully fetched {} messages from user: {}", messages.size(), fromUserId);
            return messages;

        } catch (Exception e) {
            log.error("Error occurred while fetching messages from user: {}", fromUserId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Найти все сообщения для конкретного пользователя.
     * Использует индекс для быстрого поиска.
     *
     * @param toUserId ID получателя
     * @return список сообщений
     */
    @Override
    public List<Message> findAllByTo(final UUID toUserId) {
        log.debug("Fetching all messages to user: {}", toUserId);

        if (Guard.isNullOrEmpty(toUserId)) {
            log.warn("FindAllByFrom called with null or empty toUserId");
            return Collections.emptyList();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            final var toIndexKey = USER_TO_INDEX_PREFIX + toUserId;
            final var messageIds = jedis.smembers(toIndexKey);

            if (messageIds.isEmpty()) {
                log.debug("No messages found from user: {}", toUserId);
                return Collections.emptyList();
            }

            log.debug("Found {} message IDs to user: {}, fetching messages",
                    messageIds.size(), toUserId);

            // Получаем сообщения через pipeline
            final var messages = getMessagesByIds(jedis, messageIds);

            log.info("Successfully fetched {} messages to user: {}", messages.size(), toUserId);
            return messages;

        } catch (Exception e) {
            log.error("Error occurred while fetching messages to user: {}", toUserId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Найти переписку между двумя пользователями.
     *
     * @param user1Id ID первого пользователя
     * @param user2Id ID второго пользователя
     * @return список сообщений, отсортированных по дате
     */
    @Override
    public List<Message> findConversation(final UUID user1Id, final UUID user2Id) {
        log.debug("Fetching conversation between users: {} and {}", user1Id, user2Id);

        if (Guard.isNullOrEmpty(user1Id)) {
            log.warn("FindConversation called with null or empty user1Id");
            return Collections.emptyList();
        }

        if (Guard.isNullOrEmpty(user2Id)) {
            log.warn("FindConversation called with null or empty user2Id");
            return Collections.emptyList();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            final String from1Key = USER_FROM_INDEX_PREFIX + user1Id;
            final String to1Key = USER_TO_INDEX_PREFIX + user2Id;
            final String from2Key = USER_FROM_INDEX_PREFIX + user2Id;
            final String to2Key = USER_TO_INDEX_PREFIX + user1Id;

            // Получаем сообщения от user1 к user2
            final var messages1to2 = jedis.sinter(from1Key, to1Key);

            // Получаем сообщения от user2 к user1
            final var messages2to1 = jedis.sinter(from2Key, to2Key);

            Set<String> allMessageIds = new HashSet<>();
            allMessageIds.addAll(messages1to2);
            allMessageIds.addAll(messages2to1);

            if (allMessageIds.isEmpty()) {
                log.debug("No conversation found between users: {} and {}", user1Id, user2Id);
                return Collections.emptyList();
            }

            log.debug("Found {} messages in conversation between users: {} and {}",
                    allMessageIds.size(), user1Id, user2Id);

            final var messages = getMessagesByIds(jedis, allMessageIds);
            // Сортируем по дате
            messages.sort(Comparator.comparing(Message::getDate));

            log.info("Successfully fetched {} messages in conversation between users: {} and {}",
                    messages.size(), user1Id, user2Id);
            return messages;
        } catch (Exception e) {
            log.error("Error occurred while fetching conversation between users: {} and {}",
                    user1Id, user2Id, e);
            return Collections.emptyList();
        }
    }

    /**
     * Подсчитать количество сообщений от пользователя.
     *
     * @param fromUserId ID отправителя
     * @return количество сообщений
     */
    @Override
    public long countByFrom(final UUID fromUserId) {
        log.debug("Counting messages from user: {}", fromUserId);

        if (Guard.isNullOrEmpty(fromUserId)) {
            log.warn("CountByFrom called with null or empty fromUserId");
            return 0L;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            final var fromIndexKey = USER_FROM_INDEX_PREFIX + fromUserId;
            final var count = jedis.scard(fromIndexKey);

            log.debug("User {} has {} messages sent", fromUserId, count);
            return count;

        } catch (Exception e) {
            log.error("Error occurred while counting messages from user: {}", fromUserId, e);
            return 0L;
        }
    }

    /**
     * Подсчитать количество сообщений для пользователя.
     *
     * @param toUserId ID получателя
     * @return количество сообщений
     */
    @Override
    public long countByTo(final UUID toUserId) {
        log.debug("Counting messages to user: {}", toUserId);

        if (Guard.isNullOrEmpty(toUserId)) {
            log.warn("CountByTo called with null or empty toUserId");
            return 0L;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            final var toIndexKey = USER_TO_INDEX_PREFIX + toUserId;
            final var count = jedis.scard(toIndexKey);

            log.debug("User {} has {} messages received", toUserId, count);
            return count;

        } catch (Exception e) {
            log.error("Error occurred while counting messages to user: {}", toUserId, e);
            return 0L;
        }
    }

    /**
     * Проверить существование сообщения.
     *
     * @param id идентификатор сообщения
     * @return true если сообщение существует
     */
    @Override
    public boolean existsById(final UUID id) {
        log.debug("Checking if message exists with id: {}", id);

        if (Guard.isNullOrEmpty(id)) {
            log.warn("ExistsById called with null or empty id");
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            final String messageKey = MESSAGE_KEY_PREFIX + id;
            boolean exists = jedis.exists(messageKey);

            log.debug("Message {} exists: {}", id, exists);
            return exists;

        } catch (Exception e) {
            log.error("Error occurred while checking message existence: {}", id, e);
            return false;
        }
    }

    /**
     * Удалить все сообщения (ОСТОРОЖНО!).
     * Удаляет все сообщения и все индексы.
     *
     * @return UnitResult с результатом операции
     */
    @Override
    public UnitResult<Error> deleteAll() {
        log.warn("Attempting to delete ALL messages from Redis");

        try (Jedis jedis = jedisPool.getResource()) {
            // Получаем все ID сообщений
            final var allMessageIds = jedis.smembers(ALL_MESSAGES_KEY);

            if (allMessageIds.isEmpty()) {
                log.info("No messages to delete");
                return UnitResult.success();
            }

            // Получаем все ключи для удаления
            HashSet<String> allKeys = new HashSet<>();

            // Добавляем ключи сообщений
            for (String id : allMessageIds) {
                allKeys.add(MESSAGE_KEY_PREFIX + id);
            }

            // Добавляем ключи индексов (получаем все ключи user:messages:*)
            Set<String> indexKeys = jedis.keys(USER_FROM_INDEX_PREFIX + "*");
            allKeys.addAll(indexKeys);

            indexKeys = jedis.keys(USER_TO_INDEX_PREFIX + "*");
            allKeys.addAll(indexKeys);

            // Добавляем общий индекс
            allKeys.add(ALL_MESSAGES_KEY);

            // Удаляем все через pipeline
            final var pipeline = jedis.pipelined();
            for (String key : allKeys) {
                pipeline.del(key);
            }
            pipeline.sync();

            log.warn("Successfully deleted {} messages and all indexes", allMessageIds.size());
            return UnitResult.success();

        } catch (Exception e) {
            log.error("Error occurred while deleting all messages", e);
            return UnitResult.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Получить сообщения по списку ID через pipeline (batch).
     * Оптимизирует количество обращений к Redis.
     *
     * @param jedis      подключение к Redis
     * @param messageIds Set с ID сообщений
     * @return список сообщений
     */
    private List<Message> getMessagesByIds(final Jedis jedis, final Set<String> messageIds) {
        if (messageIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Используем pipeline для batch-получения всех сообщений за один round-trip
        Pipeline pipeline = jedis.pipelined();
        List<Response<String>> responses = new ArrayList<>();

        for (String messageId : messageIds) {
            String messageKey = MESSAGE_KEY_PREFIX + messageId;
            responses.add(pipeline.get(messageKey));
        }

        // Выполняем все запросы одним батчем
        pipeline.sync();

        return responses.stream()
                .map(Response::get)
                .filter(Objects::nonNull)
                .filter(json -> !json.isEmpty())
                .map(json -> {
                    try {
                        return gson.fromJson(json, Message.class);
                    } catch (Exception e) {
                        log.error("Failed to deserialize message from JSON", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}

