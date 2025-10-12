package ru.test.the.best.chat.core.repository;

import ru.test.the.best.chat.errs.Error;
import ru.test.the.best.chat.errs.Result;
import ru.test.the.best.chat.errs.UnitResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Repository<T, I> {

    /**
     * Сохранить сообщение с индексацией.
     *
     * @param message сообщение для сохранения
     * @return UnitResult с результатом операции
     */
    UnitResult<Error> save(final T message);

    /**
     * Найти сообщение по ID.
     *
     * @param id идентификатор сообщения
     * @return Result с Optional<Message> или Error
     */
    Result<Optional<T>, Error> findById(final I id);

    /**
     * Найти все сообщения.
     * ВНИМАНИЕ: Может быть медленным при большом количестве сообщений!
     *
     * @return список всех сообщений
     */
    List<T> findAll();

    /**
     * Удалить сообщение по ID.
     * Удаляет из основного хранилища и всех индексов.
     *
     * @param id идентификатор сообщения
     * @return UnitResult с результатом операции
     */
    UnitResult<Error> deleteById(final I id);

    /**
     * Найти все сообщения от конкретного пользователя.
     * Использует индекс для быстрого поиска.
     *
     * @param fromUserId ID отправителя
     * @return список сообщений
     */
    List<T> findAllByFrom(final UUID fromUserId);

    /**
     * Найти все сообщения для конкретного пользователя.
     * Использует индекс для быстрого поиска.
     *
     * @param toUserId ID получателя
     * @return список сообщений
     */
    List<T> findAllByTo(final UUID toUserId);

    /**
     * Найти переписку между двумя пользователями.
     *
     * @param user1Id ID первого пользователя
     * @param user2Id ID второго пользователя
     * @return список сообщений, отсортированных по дате
     */
    List<T> findConversation(final UUID user1Id, final UUID user2Id);

    /**
     * Подсчитать количество сообщений от пользователя.
     *
     * @param fromUserId ID отправителя
     * @return количество сообщений
     */
    long countByFrom(final I fromUserId);

    /**
     * Подсчитать количество сообщений для пользователя.
     *
     * @param toUserId ID получателя
     * @return количество сообщений
     */
    long countByTo(final UUID toUserId);

    /**
     * Проверить существование сообщения.
     *
     * @param id идентификатор сообщения
     * @return true если сообщение существует
     */
    boolean existsById(final I id);

    /**
     * Удалить все сообщения (ОСТОРОЖНО!).
     * Удаляет все сообщения и все индексы.
     *
     * @return UnitResult с результатом операции
     */
    UnitResult<Error> deleteAll();
}

