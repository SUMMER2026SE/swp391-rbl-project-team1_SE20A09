CREATE TABLE chat_message_hidden_by (
    message_id BIGINT NOT NULL REFERENCES chat_messages(message_id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    hidden_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (message_id, user_id)
);
