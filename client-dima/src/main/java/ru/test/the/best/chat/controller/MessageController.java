package ru.test.the.best.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.test.the.best.chat.errs.api.ApiResultResponse;
import ru.test.the.best.chat.model.dto.message.CreateMessageRequest;
import ru.test.the.best.chat.model.dto.message.MessageResponse;
import ru.test.the.best.chat.service.MessageService;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "API для управления сообщениями")
public class MessageController {

    private final MessageService messageService;

    @Operation(
            summary = "Получить все сообщения",
            description = "Возвращает список всех сообщений в системе"
    )
    @GetMapping
    public ResponseEntity<ApiResultResponse<List<MessageResponse>>> getAllMessages() {
        log.info("Start get all messages");

        try {
            List<MessageResponse> messages = messageService.findAll();
            log.info("End get all messages");

            return ResponseEntity.ok(ApiResultResponse.success(messages));
        } catch (Exception e) {
            log.error("End get all messages", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResultResponse.failure("internal.error", "Failed to fetch messages"));
        }
    }

    @Operation(
            summary = "Получить сообщение по ID",
            description = "Возвращает данные сообщения по его уникальному идентификатору"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResultResponse<MessageResponse>> getMessageById(
            @Parameter(description = "UUID сообщения", required = true)
            @PathVariable UUID id) {

        log.info("REST: GET /api/v1/messages/{} - Fetching message by id", id);

        var result = messageService.findById(id);

        if (result.isFailure()) {
            log.warn("REST: Failed to fetch message with id: {}", id);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResultResponse.failure(
                            result.getError().getCode(),
                            result.getError().getMessage()
                    ));
        }

        if (result.getValue().isEmpty()) {
            log.warn("REST: Message not found with id: {}", id);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResultResponse.failure(
                            "message.not.found",
                            "Message not found with id: " + id
                    ));
        }

        log.info("REST: Successfully fetched message with id: {}", id);
        return ResponseEntity.ok(ApiResultResponse.success(result.getValue().get()));
    }

    /**
     * Получить все сообщения от конкретного пользователя.
     * <p>
     * GET /api/v1/messages?from={userId}
     */
    @Operation(
            summary = "Получить сообщения от пользователя",
            description = "Возвращает все сообщения, отправленные конкретным пользователем"
    )
    @GetMapping(params = "from")
    public ResponseEntity<ApiResultResponse<List<MessageResponse>>> getMessagesByFrom(
            @Parameter(description = "UUID отправителя", required = true)
            @RequestParam("from") UUID fromUserId) {

        log.info("REST: GET /api/v1/messages?from={} - Fetching messages from user", fromUserId);

        var result = messageService.findAllByFrom(fromUserId);

        if (result.isFailure()) {
            log.warn("REST: Failed to fetch messages from user: {}", fromUserId);
            HttpStatus status = determineHttpStatus(result.getError().getCode());
            return ResponseEntity
                    .status(status)
                    .body(ApiResultResponse.failure(
                            result.getError().getCode(),
                            result.getError().getMessage()
                    ));
        }

        log.info("REST: Successfully fetched {} messages from user: {}",
                result.getValue().size(), fromUserId);
        return ResponseEntity.ok(ApiResultResponse.success(result.getValue()));
    }

    /**
     * Получить все сообщения для конкретного пользователя.
     * <p>
     * GET /api/v1/messages?to={userId}
     */
    @Operation(
            summary = "Получить сообщения для пользователя",
            description = "Возвращает все сообщения, адресованные конкретному пользователю"
    )
    @GetMapping(params = "to")
    public ResponseEntity<ApiResultResponse<List<MessageResponse>>> getMessagesByTo(
            @Parameter(description = "UUID получателя", required = true)
            @RequestParam("to") UUID toUserId) {

        log.info("REST: GET /api/v1/messages?to={} - Fetching messages to user", toUserId);

        var result = messageService.findAllByTo(toUserId);

        if (result.isFailure()) {
            log.warn("REST: Failed to fetch messages to user: {}", toUserId);
            HttpStatus status = determineHttpStatus(result.getError().getCode());
            return ResponseEntity
                    .status(status)
                    .body(ApiResultResponse.failure(
                            result.getError().getCode(),
                            result.getError().getMessage()
                    ));
        }

        log.info("REST: Successfully fetched {} messages to user: {}",
                result.getValue().size(), toUserId);
        return ResponseEntity.ok(ApiResultResponse.success(result.getValue()));
    }

    /**
     * Получить переписку между двумя пользователями.
     * <p>
     * GET /api/v1/messages/conversation?user1={id}&user2={id}
     */
    @Operation(
            summary = "Получить переписку между пользователями",
            description = "Возвращает все сообщения между двумя пользователями, отсортированные по дате"
    )
    @GetMapping("/conversation")
    public ResponseEntity<ApiResultResponse<List<MessageResponse>>> getConversation(
            @Parameter(description = "UUID первого пользователя", required = true)
            @RequestParam("user1") UUID user1Id,
            @Parameter(description = "UUID второго пользователя", required = true)
            @RequestParam("user2") UUID user2Id) {

        log.info("REST: GET /api/v1/messages/conversation?user1={}&user2={} - Fetching conversation",
                user1Id, user2Id);

        var result = messageService.findConversation(user1Id, user2Id);

        if (result.isFailure()) {
            log.warn("REST: Failed to fetch conversation between users: {} and {}",
                    user1Id, user2Id);
            HttpStatus status = determineHttpStatus(result.getError().getCode());
            return ResponseEntity
                    .status(status)
                    .body(ApiResultResponse.failure(
                            result.getError().getCode(),
                            result.getError().getMessage()
                    ));
        }

        log.info("REST: Successfully fetched {} messages in conversation between {} and {}",
                result.getValue().size(), user1Id, user2Id);
        return ResponseEntity.ok(ApiResultResponse.success(result.getValue()));
    }

    /**
     * Отправить сообщение.
     * <p>
     * POST /api/v1/messages
     */
    @Operation(
            summary = "Отправить сообщение",
            description = "Создает и отправляет новое сообщение"
    )
    @PostMapping
    public ResponseEntity<ApiResultResponse<Void>> sendMessage(
            @Valid @RequestBody CreateMessageRequest request) {

        log.info("REST: POST /api/v1/messages - Sending message from {} to {}",
                request.from(), request.to());

        var result = messageService.save(request);

        if (result.isFailure()) {
            var error = result.getError();
            log.warn("REST: Failed to send message: {} - {}", error.getCode(), error.getMessage());

            HttpStatus status = determineHttpStatus(error.getCode());

            return ResponseEntity
                    .status(status)
                    .body(ApiResultResponse.failure(error.getCode(), error.getMessage()));
        }

        log.info("REST: Successfully sent message from {} to {}",
                request.from(), request.to());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResultResponse.success(null));
    }

    /**
     * Обновить сообщение.
     * <p>
     * PUT /api/v1/messages/{id}
     */
    @Operation(
            summary = "Обновить сообщение",
            description = "Обновляет содержимое существующего сообщения"
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResultResponse<Void>> updateMessage(
            @Parameter(description = "UUID сообщения", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody CreateMessageRequest request) {

        log.info("REST: PUT /api/v1/messages/{} - Updating message", id);

        var result = messageService.updateById(id, request);

        if (result.isFailure()) {
            var error = result.getError();
            log.warn("REST: Failed to update message {}: {} - {}",
                    id, error.getCode(), error.getMessage());

            HttpStatus status = determineHttpStatus(error.getCode());

            return ResponseEntity
                    .status(status)
                    .body(ApiResultResponse.failure(error.getCode(), error.getMessage()));
        }

        log.info("REST: Successfully updated message with id: {}", id);
        return ResponseEntity.ok(ApiResultResponse.success(null));
    }

    /**
     * Удалить сообщение.
     * <p>
     * DELETE /api/v1/messages/{id}
     */
    @Operation(
            summary = "Удалить сообщение",
            description = "Удаляет сообщение из системы"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResultResponse<Void>> deleteMessage(
            @Parameter(description = "UUID сообщения", required = true)
            @PathVariable UUID id) {

        log.info("REST: DELETE /api/v1/messages/{} - Deleting message", id);

        var result = messageService.deleteById(id);

        if (result.isFailure()) {
            var error = result.getError();
            log.warn("REST: Failed to delete message {}: {} - {}",
                    id, error.getCode(), error.getMessage());

            HttpStatus status = determineHttpStatus(error.getCode());

            return ResponseEntity
                    .status(status)
                    .body(ApiResultResponse.failure(error.getCode(), error.getMessage()));
        }

        log.info("REST: Successfully deleted message with id: {}", id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    private HttpStatus determineHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "entity.not.found", "record.not.found", "message.not.found" -> HttpStatus.NOT_FOUND;
            case "duplicate.entity", "data.conflict" -> HttpStatus.CONFLICT;
            case "validation.error", "value.is.invalid", "value.is.empty",
                 "value.is.required", "invalid.string.length", "invalid.format" -> HttpStatus.BAD_REQUEST;
            case "access.denied" -> HttpStatus.FORBIDDEN;
            case "authentication.failed", "invalid.token" -> HttpStatus.UNAUTHORIZED;
            case "database.error", "internal.server.error" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
