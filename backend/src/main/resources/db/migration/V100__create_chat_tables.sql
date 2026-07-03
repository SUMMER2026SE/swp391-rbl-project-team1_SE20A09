-- ═══════════════════════════════════════════════════════════════
-- V7__create_chat_tables.sql
-- Real-time chat: conversations + messages
-- ═══════════════════════════════════════════════════════════════

-- 1-on-1 chat conversations
CREATE TABLE IF NOT EXISTS chat_conversations (
    conversation_id  BIGSERIAL PRIMARY KEY,
    user1_id         INT NOT NULL REFERENCES users(user_id),
    user2_id         INT NOT NULL REFERENCES users(user_id),
    last_message_preview VARCHAR(255),
    last_message_at  TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Canonical ordering: user1_id < user2_id avoids duplicate conversations
    CONSTRAINT uk_chat_conv_users UNIQUE (user1_id, user2_id),
    CONSTRAINT ck_chat_conv_user_order CHECK (user1_id < user2_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_conv_user1 ON chat_conversations(user1_id);
CREATE INDEX IF NOT EXISTS idx_chat_conv_user2 ON chat_conversations(user2_id);
CREATE INDEX IF NOT EXISTS idx_chat_conv_updated ON chat_conversations(last_message_at DESC);

-- Individual chat messages
CREATE TABLE IF NOT EXISTS chat_messages (
    message_id       BIGSERIAL PRIMARY KEY,
    conversation_id  BIGINT NOT NULL REFERENCES chat_conversations(conversation_id) ON DELETE CASCADE,
    sender_id        INT NOT NULL REFERENCES users(user_id),
    content          TEXT NOT NULL,
    message_type     VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    is_read          BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_msg_conv_time ON chat_messages(conversation_id, sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_msg_sender ON chat_messages(sender_id);

-- Chatbot conversation logs (for AI assistant)
CREATE TABLE IF NOT EXISTS chatbot_logs (
    log_id           BIGSERIAL PRIMARY KEY,
    user_id          INT NOT NULL REFERENCES users(user_id),
    user_message     TEXT NOT NULL,
    bot_response     TEXT NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chatbot_logs_user ON chatbot_logs(user_id, created_at DESC);
