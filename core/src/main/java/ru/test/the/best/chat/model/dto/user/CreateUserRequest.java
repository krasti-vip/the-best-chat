package ru.test.the.best.chat.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9_-]+$",
                message = "Username can only contain letters, numbers, underscore and hyphen"
        )
        String username
) {
}
