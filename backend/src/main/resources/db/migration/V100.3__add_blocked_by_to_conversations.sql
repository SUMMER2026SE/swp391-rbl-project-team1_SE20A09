-- Add blocked_by column to chat_conversations
-- Stores the user_id of the person who initiated the block (2-way mutual block)
ALTER TABLE chat_conversations ADD COLUMN blocked_by INTEGER REFERENCES users(user_id);
ALTER TABLE chat_conversations ADD COLUMN blocked_at TIMESTAMP;

CREATE INDEX idx_chat_conv_blocked ON chat_conversations(blocked_by) WHERE blocked_by IS NOT NULL;
