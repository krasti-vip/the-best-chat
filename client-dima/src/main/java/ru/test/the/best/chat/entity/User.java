package ru.test.the.best.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.extern.slf4j.Slf4j;
import ru.test.the.best.chat.errs.Error;
import ru.test.the.best.chat.errs.GeneralErrors;
import ru.test.the.best.chat.errs.Guard;
import ru.test.the.best.chat.errs.Result;
import ru.test.the.best.chat.model.dto.user.UserResponse;

import java.util.UUID;

@Slf4j
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "username", nullable = false)
    private String username;

    public User() {
    }

    public User(final String username) {
        this.id = UUID.randomUUID();
        this.username = username;
    }

    public static Result<User, Error> create(String username) {

        if (Guard.isNull(username))
            return Result.failure(GeneralErrors.valueIsEmpty("username is null"));

        return Result.success(new User(username));
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserResponse toUserResponse() {
        return new UserResponse(this.id, this.username);
    }
}
