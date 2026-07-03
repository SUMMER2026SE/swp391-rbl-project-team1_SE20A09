-- Create a bridging table to track which users have read which messages in group chats
CREATE TABLE chat_message_read_by (
    message_id BIGINT NOT NULL,
    user_id INTEGER NOT NULL,
    PRIMARY KEY (message_id, user_id),
    CONSTRAINT fk_read_by_message FOREIGN KEY (message_id) REFERENCES chat_messages(message_id) ON DELETE CASCADE,
    CONSTRAINT fk_read_by_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
