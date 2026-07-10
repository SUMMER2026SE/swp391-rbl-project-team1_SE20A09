ALTER TABLE ai_usage_log
ADD COLUMN user_input TEXT,
ADD COLUMN raw_llm_response TEXT,
ADD COLUMN parsed_intent VARCHAR(50),
ADD COLUMN resolved_stadium_id INT,
ADD COLUMN resolved_slot_id INT,
ADD COLUMN action_result TEXT;
