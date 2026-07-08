CREATE TABLE ai_usage_log (
    id BIGSERIAL PRIMARY KEY,
    user_id INT,
    feature VARCHAR(50),
    model_used VARCHAR(100),
    input_tokens INT,
    output_tokens INT,
    latency_ms BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_usage_log_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE INDEX idx_ai_usage_log_user_id ON ai_usage_log(user_id);
CREATE INDEX idx_ai_usage_log_feature ON ai_usage_log(feature);
CREATE INDEX idx_ai_usage_log_created_at ON ai_usage_log(created_at);
