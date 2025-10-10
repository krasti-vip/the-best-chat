package ru.test.the.best.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.test.the.best.chat.model.entity.User;

import java.util.UUID;

@Repository
public interface PostgresUserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);
}
