ALTER TABLE chat_conversations ADD COLUMN IF NOT EXISTS user1_nickname VARCHAR(100);
ALTER TABLE chat_conversations ADD COLUMN IF NOT EXISTS user2_nickname VARCHAR(100);
