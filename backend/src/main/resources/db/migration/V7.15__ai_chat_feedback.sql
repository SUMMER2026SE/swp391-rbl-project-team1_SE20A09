CREATE TABLE IF NOT EXISTS ai_chat_feedback (
    feedback_id SERIAL PRIMARY KEY,
    message_id VARCHAR(100) NOT NULL,
    user_id INT REFERENCES users(user_id) ON DELETE SET NULL,
    session_id VARCHAR(100),
    rating VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
