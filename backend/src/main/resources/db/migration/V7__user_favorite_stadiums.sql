-- Sân yêu thích của người dùng (lưu DB, giống avatar/profile)
CREATE TABLE user_favorite_stadiums (
    id          BIGSERIAL PRIMARY KEY,
    user_id     INT         NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    stadium_id  INT         NOT NULL REFERENCES stadiums(stadium_id) ON DELETE CASCADE,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_favorite_stadium UNIQUE (user_id, stadium_id)
);

CREATE INDEX idx_user_favorites_user_id ON user_favorite_stadiums(user_id);

-- Mẫu cho customer@sportvenue.com (password trong DataSeeder / V4)
INSERT INTO user_favorite_stadiums (user_id, stadium_id)
SELECT u.user_id, s.stadium_id
FROM users u
CROSS JOIN stadiums s
WHERE u.email = 'customer@sportvenue.com'
  AND s.stadium_name IN ('Sân Bóng Đá Thủ Đức', 'Sân Cầu Lông Quận 1')
ON CONFLICT DO NOTHING;
