ALTER TABLE ai_usage_log
ADD COLUMN prompt_version VARCHAR(20),
ADD COLUMN confidence DOUBLE PRECISION,
ADD COLUMN rule_override BOOLEAN,
ADD COLUMN validation_result VARCHAR(50),
ADD COLUMN error_reason TEXT,
ADD COLUMN processing_time_ai_ms BIGINT,
ADD COLUMN processing_time_handler_ms BIGINT,
ADD COLUMN redis_hit BOOLEAN,
ADD COLUMN handler_name VARCHAR(100);
