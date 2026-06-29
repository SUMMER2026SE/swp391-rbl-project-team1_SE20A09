-- 1. Remove the strict unique constraint on 1-on-1 chats to allow the same users to be in multiple groups
ALTER TABLE chat_conversations DROP CONSTRAINT IF EXISTS uk_chat_conv_users;

-- 2. Add columns to support group chat features
ALTER TABLE chat_conversations 
ADD COLUMN name VARCHAR(255) NULL,
ADD COLUMN is_group BOOLEAN DEFAULT FALSE,
ADD COLUMN match_id INTEGER NULL;

ALTER TABLE chat_conversations
ADD CONSTRAINT fk_chat_match FOREIGN KEY (match_id) REFERENCES match_requests(id) ON DELETE SET NULL;

-- 3. Create a bridging table for group chat members
CREATE TABLE chat_group_members (
    conversation_id BIGINT NOT NULL,
    user_id INTEGER NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_group_member_conv FOREIGN KEY (conversation_id) REFERENCES chat_conversations(conversation_id) ON DELETE CASCADE,
    CONSTRAINT fk_group_member_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Make user1_id and user2_id nullable, because a group chat doesn't strictly have just two specific users
ALTER TABLE chat_conversations ALTER COLUMN user1_id DROP NOT NULL;
ALTER TABLE chat_conversations ALTER COLUMN user2_id DROP NOT NULL;
