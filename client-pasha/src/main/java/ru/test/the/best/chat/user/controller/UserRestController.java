package ru.test.the.best.chat.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.test.the.best.chat.errs.api.ApiResultResponse;
import ru.test.the.best.chat.model.dto.user.CreateUserRequest;
import ru.test.the.best.chat.model.dto.user.UserResponse;
import ru.test.the.best.chat.user.service.UserService;

import java.util.List;
import java.util.UUID;

/**
 * Base URL: /api/v1/users
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "API для управления пользователями")
public class UserRestController {

    private final UserService userService;

    /**
     * Получить список всех пользователей.
     *
     * GET /api/v1/users
     */
    @Operation(
            summary = "Получить всех пользователей",
            description = "Возвращает список всех зарегистрированных пользователей в системе"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешно получен список пользователей",
                    content = @Content(schema = @Schema(implementation = ApiResultResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера"
            )
    })
    @GetMapping
    public ResponseEntity<ApiResultResponse<List<UserResponse>>> getAllUsers() {
        log.info("REST: GET /api/v1/users - Fetching all users");

        try {
            List<UserResponse> users = userService.findAll();
            log.info("REST: Successfully fetched {} users", users.size());

            return ResponseEntity.ok(
                    ApiResultResponse.success(users)
            );
        } catch (Exception e) {
            log.error("REST: Error fetching all users", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResultResponse.failure("internal.error", "Failed to fetch users"));
        }
    }

    /**
     * Получить пользователя по ID.
     *
     * GET /api/v1/users/{id}
     */
    @Operation(
            summary = "Получить пользователя по ID",
            description = "Возвращает данные пользователя по его уникальному идентификатору"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь найден",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидный ID"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResultResponse<UserResponse>> getUserById(
            @Parameter(description = "UUID пользователя", required = true)
            @PathVariable UUID id) {

        log.info("REST: GET /api/v1/users/{} - Fetching user by id", id);

        var result = userService.findById(id);

        if (result.isFailure()) {
            log.warn("REST: Failed to fetch user with id: {}", id);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResultResponse.failure(
                            result.getError().getCode(),
                            result.getError().getMessage()
                    ));
        }

        if (result.getValue().isEmpty()) {
            log.warn("REST: User not found with id: {}", id);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResultResponse.failure(
                            "user.not.found",
                            "User not found with id: " + id
                    ));
        }

        log.info("REST: Successfully fetched user with id: {}", id);
        return ResponseEntity.ok(
                ApiResultResponse.success(result.getValue().get())
        );
    }

    /**
     * Создать нового пользователя.
     *
     * POST /api/v1/users
     */
    @Operation(
            summary = "Создать нового пользователя",
            description = "Регистрирует нового пользователя в системе"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Пользователь успешно создан"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидные данные запроса"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Пользователь с таким username уже существует"
            )
    })
    @PostMapping
    public ResponseEntity<ApiResultResponse<Void>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        log.info("REST: POST /api/v1/users - Creating new user with username: {}",
                request.username());

        var result = userService.save(request);

        if (result.isFailure()) {
            var error = result.getError();
            log.warn("REST: Failed to create user: {} - {}", error.getCode(), error.getMessage());

            // Определяем HTTP статус по коду ошибки
            HttpStatus status = determineHttpStatus(error.getCode());

            return ResponseEntity
                    .status(status)
                    .body(ApiResultResponse.failure(error.getCode(), error.getMessage()));
        }

        log.info("REST: Successfully created user with username: {}", request.username());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResultResponse.success(null));
    }

    /**
     * Обновить данные пользователя.
     *
     * PUT /api/v1/users/{id}
     */
    @Operation(
            summary = "Обновить пользователя",
            description = "Обновляет данные существующего пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно обновлен"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидные данные запроса"
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResultResponse<Void>> updateUser(
            @Parameter(description = "UUID пользователя", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody CreateUserRequest request) {

        log.info("REST: PUT /api/v1/users/{} - Updating user", id);

        var result = userService.update(id, request);

        if (result.isFailure()) {
            var error = result.getError();
            log.warn("REST: Failed to update user {}: {} - {}",
                    id, error.getCode(), error.getMessage());

            HttpStatus status = determineHttpStatus(error.getCode());

            return ResponseEntity
                    .status(status)
                    .body(ApiResultResponse.failure(error.getCode(), error.getMessage()));
        }

        log.info("REST: Successfully updated user with id: {}", id);
        return ResponseEntity.ok(ApiResultResponse.success(null));
    }

    /**
     * Удалить пользователя.
     *
     * DELETE /api/v1/users/{id}
     */
    @Operation(
            summary = "Удалить пользователя",
            description = "Удаляет пользователя из системы"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Пользователь успешно удален"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResultResponse<Void>> deleteUser(
            @Parameter(description = "UUID пользователя", required = true)
            @PathVariable UUID id) {

        log.info("REST: DELETE /api/v1/users/{} - Deleting user", id);

        var result = userService.deleteById(id);

        if (result.isFailure()) {
            var error = result.getError();
            log.warn("REST: Failed to delete user {}: {} - {}",
                    id, error.getCode(), error.getMessage());

            HttpStatus status = determineHttpStatus(error.getCode());

            return ResponseEntity
                    .status(status)
                    .body(ApiResultResponse.failure(error.getCode(), error.getMessage()));
        }

        log.info("REST: Successfully deleted user with id: {}", id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    /**
     * Проверить существование пользователя.
     *
     * HEAD /api/v1/users/{id}
     */
    @Operation(
            summary = "Проверить существование пользователя",
            description = "Проверяет, существует ли пользователь с данным ID"
    )
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkUserExists(@PathVariable UUID id) {
        log.info("REST: HEAD /api/v1/users/{} - Checking user existence", id);

        var result = userService.findById(id);

        if (result.isSuccess() && result.getValue().isPresent()) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Определяет HTTP статус по коду ошибки.
     */
    private HttpStatus determineHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "entity.not.found", "record.not.found", "user.not.found" ->
                    HttpStatus.NOT_FOUND;
            case "duplicate.entity", "data.conflict" ->
                    HttpStatus.CONFLICT;
            case "validation.error", "value.is.invalid", "value.is.empty",
                 "value.is.required", "invalid.string.length", "invalid.format" ->
                    HttpStatus.BAD_REQUEST;
            case "access.denied" ->
                    HttpStatus.FORBIDDEN;
            case "authentication.failed", "invalid.token" ->
                    HttpStatus.UNAUTHORIZED;
            case "database.error", "internal.server.error" ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
            default ->
                    HttpStatus.BAD_REQUEST;
        };
    }
}

