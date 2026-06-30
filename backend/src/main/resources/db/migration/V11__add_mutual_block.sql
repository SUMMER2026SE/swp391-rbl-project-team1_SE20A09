-- Add mutual block state to chat_conversations
ALTER TABLE chat_conversations ADD COLUMN is_mutual_block BOOLEAN DEFAULT FALSE;
