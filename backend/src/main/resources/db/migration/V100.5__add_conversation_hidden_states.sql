CREATE TABLE chat_conversation_hidden_by (
    conversation_id BIGINT NOT NULL REFERENCES chat_conversations(conversation_id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    hidden_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (conversation_id, user_id)
);
