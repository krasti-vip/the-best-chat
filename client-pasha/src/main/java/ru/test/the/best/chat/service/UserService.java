package ru.test.the.best.chat.service;

import ru.test.the.best.chat.errs.*;
import ru.test.the.best.chat.errs.Error;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.test.the.best.chat.model.dto.user.CreateUserRequest;
import ru.test.the.best.chat.model.dto.user.UserResponse;
import ru.test.the.best.chat.model.entity.User;
import ru.test.the.best.chat.repository.PostgresUserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // По умолчанию все методы read-only
public class UserService implements ru.test.the.best.chat.service.Service<UserResponse, UUID, CreateUserRequest> {

    private final PostgresUserRepository userRepository;

    /**
     * Получить всех пользователей.
     *
     * @return список всех пользователей в системе
     */
    @Override
    public List<UserResponse> findAll() {
        log.debug("Fetching all users from database");

        try {
            final List<UserResponse> users = userRepository.findAll().stream()
                    .map(User::toUserResponse)
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} users", users.size());
            return users;
        } catch (Exception e) {
            log.error("Error occurred while fetching all users", e);
            return Collections.emptyList();
        }
    }

    /**
     * Найти пользователя по ID.
     *
     * @param id уникальный идентификатор пользователя
     * @return Result с Optional<UserResponse> или Error
     */
    @Override
    public Result<Optional<UserResponse>, Error> findById(final UUID id) {
        log.debug("Attempting to find user by id: {}", id);

        if (Guard.isNullOrEmpty(id)) {
            log.warn("FindById called with null or empty id");
            return Result.failure(GeneralErrors.valueIsEmpty("id"));
        }

        try {
            final Optional<UserResponse> userResponse = userRepository.findById(id)
                    .map(User::toUserResponse);

            if (userResponse.isPresent()) {
                log.info("User found with id: {}", id);
            } else {
                log.warn("User not found with id: {}", id);
            }

            return Result.success(userResponse);
        } catch (Exception e) {
            log.error("Error occurred while finding user by id: {}", id, e);
            return Result.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    /**
     * Удалить пользователя по ID.
     *
     * @param id уникальный идентификатор пользователя
     * @return UnitResult с результатом операции
     */
    @Override
    @Transactional // Включаем транзакцию для операции записи
    public UnitResult<Error> deleteById(final UUID id) {
        log.debug("Attempting to delete user with id: {}", id);

        if (Guard.isNullOrEmpty(id)) {
            log.warn("DeleteById called with null or empty id");
            return UnitResult.failure(GeneralErrors.valueIsEmpty("id"));
        }

        try {
            // Проверяем существование перед удалением
            if (!userRepository.existsById(id)) {
                log.warn("Cannot delete: user not found with id: {}", id);
                return UnitResult.failure(GeneralErrors.entityNotFound("User", id.toString()));
            }

            userRepository.deleteById(id);
            log.info("Successfully deleted user with id: {}", id);
            return UnitResult.success();
        } catch (Exception e) {
            log.error("Error occurred while deleting user with id: {}", id, e);
            return UnitResult.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    /**
     * Обновить данные существующего пользователя.
     *
     * @param id                уникальный идентификатор пользователя
     * @param createUserRequest данные для обновления
     * @return UnitResult с результатом операции
     */
    @Override
    @Transactional
    public UnitResult<Error> update(final UUID id, final CreateUserRequest createUserRequest) {
        log.debug("Attempting to update user with id: {}", id);

        // Валидация входных параметров
        if (Guard.isNullOrEmpty(id)) {
            log.warn("Update called with null or empty id");
            return UnitResult.failure(GeneralErrors.valueIsEmpty("id"));
        }

        if (Guard.isNull(createUserRequest)) {
            log.warn("Update called with null createUserRequest for id: {}", id);
            return UnitResult.failure(GeneralErrors.valueIsEmpty("createUserRequest"));
        }

        // Валидация бизнес-логики
        final UnitResult<Error> validationResult = validateUserRequest(createUserRequest);
        if (validationResult.isFailure()) {
            log.warn("Validation failed for user update with id: {}", id);
            return validationResult;
        }

        try {
            // Проверяем существование перед обновлением
            final Optional<User> existingUser = userRepository.findById(id);
            if (existingUser.isEmpty()) {
                log.warn("Cannot update: user not found with id: {}", id);
                return UnitResult.failure(GeneralErrors.entityNotFound("User", id.toString()));
            }

            // Обновляем существующую сущность
            final User userToUpdate = existingUser.get();
            userToUpdate.setUsername(createUserRequest.username());
            userRepository.save(userToUpdate);

            log.info("Successfully updated user with id: {} to username: {}", id, createUserRequest.username());
            return UnitResult.success();
        } catch (Exception e) {
            log.error("Error occurred while updating user with id: {}", id, e);
            return UnitResult.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    /**
     * Создать нового пользователя.
     *
     * @param createUserRequest данные нового пользователя
     * @return UnitResult с результатом операции
     */
    @Override
    @Transactional
    public UnitResult<Error> save(final CreateUserRequest createUserRequest) {
        log.debug("Attempting to create new user with username: {}",
                createUserRequest != null ? createUserRequest.username() : "null");

        if (Guard.isNull(createUserRequest)) {
            log.warn("Save called with null createUserRequest");
            return UnitResult.failure(GeneralErrors.valueIsEmpty("createUserRequest"));
        }

        // Валидация бизнес-логики
        final UnitResult<Error> validationResult = validateUserRequest(createUserRequest);
        if (validationResult.isFailure()) {
            log.warn("Validation failed for user creation");
            return validationResult;
        }

        try {
            // Проверяем уникальность username
            if (userRepository.existsByUsername(createUserRequest.username())) {
                log.warn("User with username '{}' already exists", createUserRequest.username());
                return UnitResult.failure(GeneralErrors.duplicateEntity("user", "username", createUserRequest.username()));
            }

            final User newUser = new User(createUserRequest.username());
            final User savedUser = userRepository.save(newUser);

            log.info("Successfully created new user with id: {} and username: {}",
                    savedUser.getId(), savedUser.getUsername());
            return UnitResult.success();
        } catch (Exception e) {
            log.error("Error occurred while creating new user with username: {}",
                    createUserRequest.username(), e);
            return UnitResult.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    /**
     * Валидация данных пользователя.
     *
     * @param request данные для валидации
     * @return UnitResult с результатом валидации
     */
    private UnitResult<Error> validateUserRequest(final CreateUserRequest request) {
        if (Guard.isNullOrEmpty(request.username())) {
            log.debug("Username is null or empty");
            return UnitResult.failure(GeneralErrors.valueIsEmpty("username"));
        }

        if (request.username().length() < 3) {
            log.debug("Username '{}' is too short (minimum 3 characters)", request.username());
            return UnitResult.failure(GeneralErrors.validationError("username", "Username must be at least 3 characters"));
        }

        if (request.username().length() > 50) {
            log.debug("Username '{}' is too long (maximum 50 characters)", request.username());
            return UnitResult.failure(GeneralErrors.validationError("username", "Username must not exceed 50 characters"));
        }

        if (!request.username().matches("^[a-zA-Z0-9_-]+$")) {
            log.debug("Username '{}' contains invalid characters", request.username());
            return UnitResult.failure(GeneralErrors.validationError("username", "Username can only contain letters, numbers, underscore and hyphen"));
        }

        return UnitResult.success();
    }
}

