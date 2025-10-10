package ru.test.the.best.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.test.the.best.chat.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);
}
