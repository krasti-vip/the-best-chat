package ru.test.the.best.chat.user.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.test.the.best.chat.core.metric.MetricOperationNameCore;
import ru.test.the.best.chat.core.metric.MetricService;
import ru.test.the.best.chat.errs.*;
import ru.test.the.best.chat.errs.Error;
import ru.test.the.best.chat.model.dto.user.CreateUserRequest;
import ru.test.the.best.chat.model.dto.user.UserResponse;
import ru.test.the.best.chat.user.model.entity.User;
import ru.test.the.best.chat.user.repository.PostgresUserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class UserService implements ru.test.the.best.chat.core.service.Service<UserResponse, UUID, CreateUserRequest> {

    private final PostgresUserRepository userRepository;

    private final MetricService metricService;

    @Autowired
    public UserService(
            final PostgresUserRepository userRepository,
            final MeterRegistry meterRegistry
    ) {
        this.userRepository = userRepository;
        this.metricService = new MetricService(meterRegistry, UserService.class, User.class);
    }

    @Override
    public List<UserResponse> findAll() {
        try {
            return metricService.timer(MetricOperationNameCore.FIND_ALL).recordCallable(() -> {
                log.debug("Fetching all users from database");

                final var users = userRepository.findAll().stream()
                        .map(User::toUserResponse)
                        .collect(Collectors.toList());

                log.info("Successfully fetched {} users", users.size());
                metricService.recordSuccess(MetricOperationNameCore.FIND_ALL);

                return users;
            });
        } catch (Exception e) {
            log.error("Error occurred while fetching all users", e);
            metricService.recordError(MetricOperationNameCore.FIND_ALL);
            return Collections.emptyList();
        }
    }

    @Override
    public Result<Optional<UserResponse>, ru.test.the.best.chat.errs.Error> findById(final UUID id) {
        try {
            return metricService.timer(MetricOperationNameCore.FIND_BY_ID).recordCallable(() -> {
                log.debug("Attempting to find user by id: {}", id);

                if (Guard.isNullOrEmpty(id)) {
                    log.warn("FindById called with null or empty id");
                    metricService.recordError(MetricOperationNameCore.FIND_BY_ID);
                    return Result.failure(GeneralErrors.valueIsEmpty("id"));
                }

                final var userResponse = userRepository.findById(id)
                        .map(User::toUserResponse);

                if (userResponse.isPresent()) {
                    log.info("User found with id: {}", id);
                    metricService.recordSuccess(MetricOperationNameCore.FIND_BY_ID);
                } else {
                    log.warn("User not found with id: {}", id);
                    metricService.recordNotFound();
                }

                return Result.success(userResponse);
            });
        } catch (Exception e) {
            log.error("Error occurred while finding user by id: {}", id, e);
            metricService.recordError(MetricOperationNameCore.FIND_BY_ID);
            return Result.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    @Override
    @Transactional
    public UnitResult<ru.test.the.best.chat.errs.Error> deleteById(final UUID id) {
        try {
            return metricService.timer(MetricOperationNameCore.DELETE_BY_ID).recordCallable(() -> {
                log.debug("Attempting to delete user with id: {}", id);

                if (Guard.isNullOrEmpty(id)) {
                    log.warn("DeleteById called with null or empty id");
                    metricService.recordError(MetricOperationNameCore.DELETE_BY_ID);
                    return UnitResult.failure(GeneralErrors.valueIsEmpty("id"));
                }

                if (!userRepository.existsById(id)) {
                    log.warn("Cannot delete: user not found with id: {}", id);
                    metricService.recordNotFound();
                    return UnitResult.failure(GeneralErrors.entityNotFound("User", id.toString()));
                }

                userRepository.deleteById(id);
                log.info("Successfully deleted user with id: {}", id);

                metricService.recordSuccess(MetricOperationNameCore.DELETE_BY_ID);

                return UnitResult.success();
            });
        } catch (Exception e) {
            log.error("Error occurred while deleting user with id: {}", id, e);
            metricService.recordError(MetricOperationNameCore.DELETE_BY_ID);
            return UnitResult.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    @Override
    @Transactional
    public UnitResult<ru.test.the.best.chat.errs.Error> update(final UUID id, final CreateUserRequest createUserRequest) {
        try {
            return metricService.timer(MetricOperationNameCore.UPDATE).recordCallable(() -> {
                log.debug("Attempting to update user with id: {}", id);

                if (Guard.isNullOrEmpty(id)) {
                    log.warn("Update called with null or empty id");
                    metricService.recordError(MetricOperationNameCore.UPDATE);
                    return UnitResult.failure(GeneralErrors.valueIsEmpty("id"));
                }

                if (Guard.isNull(createUserRequest)) {
                    log.warn("Update called with null createUserRequest for id: {}", id);
                    metricService.recordError(MetricOperationNameCore.UPDATE);
                    return UnitResult.failure(GeneralErrors.valueIsEmpty("createUserRequest"));
                }

                final UnitResult<ru.test.the.best.chat.errs.Error> validationResult = validateUserRequest(createUserRequest);
                if (validationResult.isFailure()) {
                    log.warn("Validation failed for user update with id: {}", id);
                    metricService.recordError(MetricOperationNameCore.UPDATE);
                    return validationResult;
                }

                final var existingUser = userRepository.findById(id);
                if (existingUser.isEmpty()) {
                    log.warn("Cannot update: user not found with id: {}", id);
                    metricService.recordNotFound();
                    return UnitResult.failure(GeneralErrors.entityNotFound("User", id.toString()));
                }

                final var userToUpdate = existingUser.get();

                userToUpdate.setUsername(createUserRequest.username());
                userRepository.save(userToUpdate);
                log.info("Successfully updated user with id: {} to username: {}", id, createUserRequest.username());
                metricService.recordSuccess(MetricOperationNameCore.UPDATE);

                return UnitResult.success();
            });
        } catch (Exception e) {
            log.error("Error occurred while updating user with id: {}", id, e);
            metricService.recordError(MetricOperationNameCore.UPDATE);
            return UnitResult.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

    @Override
    @Transactional
    public UnitResult<Error> save(final CreateUserRequest createUserRequest) {
        try {
            return metricService.timer(MetricOperationNameCore.SAVE).recordCallable(() -> {
                log.debug("Attempting to create new user with username: {}",
                        createUserRequest != null ? createUserRequest.username() : "null");

                if (Guard.isNull(createUserRequest)) {
                    log.warn("Save called with null createUserRequest");
                    metricService.recordError(MetricOperationNameCore.SAVE);
                    return UnitResult.failure(GeneralErrors.valueIsEmpty("createUserRequest"));
                }

                final var validationResult = validateUserRequest(createUserRequest);
                if (validationResult.isFailure()) {
                    log.warn("Validation failed for user creation");
                    return validationResult;
                }

                if (userRepository.existsByUsername(createUserRequest.username())) {
                    log.warn("User with username '{}' already exists", createUserRequest.username());
                    metricService.recordError(MetricOperationNameCore.SAVE);
                    return UnitResult.failure(GeneralErrors.duplicateEntity("user", "username", createUserRequest.username()));
                }

                final var newUser = new User(createUserRequest.username());
                final var savedUser = userRepository.save(newUser);

                log.info("Successfully created new user with id: {} and username: {}",
                        savedUser.getId(), savedUser.getUsername());

                metricService.recordSuccess(MetricOperationNameCore.SAVE);

                return UnitResult.success();
            });
        } catch (Exception e) {
            log.error("Error occurred while creating new user with username: {}",
                    createUserRequest != null ? createUserRequest.username() : "null", e);
            metricService.recordError(MetricOperationNameCore.SAVE);
            return UnitResult.failure(GeneralErrors.databaseError(e.getMessage()));
        }
    }

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