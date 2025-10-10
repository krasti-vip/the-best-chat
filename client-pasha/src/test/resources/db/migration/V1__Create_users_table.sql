-- Создание таблицы users с дополнительными ограничениями
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание индексов (Так как данный столбец UNIQUE у него и так должна быть индексация, но явно прописываем что бы точно было видно. Чем уникальнее столбец тем пизже поиск когда 1 == 1 то вообще збс)
CREATE UNIQUE INDEX idx_users_username_unique ON users(username);

-- Добавление ограничений
ALTER TABLE users
    ADD CONSTRAINT chk_username_length
        CHECK (LENGTH(TRIM(username)) >= 3);

ALTER TABLE users
    ADD CONSTRAINT chk_username_not_empty
        CHECK (TRIM(username) != '');

-- Комментарии
COMMENT ON TABLE users IS 'Таблица пользователей системы';
COMMENT ON COLUMN users.id IS 'Уникальный идентификатор пользователя';
COMMENT ON COLUMN users.username IS 'Уникальное имя пользователя (логин)';
COMMENT ON COLUMN users.created_at IS 'Дата и время создания записи';
COMMENT ON COLUMN users.updated_at IS 'Дата и время последнего обновления записи';