package ru.test.the.best.chat.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.test.the.best.chat.model.dto.user.UserResponse;

import java.util.UUID;

@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class User {

    @Id
    private UUID id;

    private String username;

    public User(final String username) {
        this(UUID.randomUUID(), username);
    }

    public UserResponse toUserResponse() {
        return new UserResponse(this.id, this.username);
    }
}
