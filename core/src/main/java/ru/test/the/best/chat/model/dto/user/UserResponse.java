package ru.test.the.best.chat.model.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UserResponse(
        @NotNull UUID id,
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9_-]+$",
                message = "Username can only contain letters, numbers, underscore and hyphen"
        )
        String username
) {

}
