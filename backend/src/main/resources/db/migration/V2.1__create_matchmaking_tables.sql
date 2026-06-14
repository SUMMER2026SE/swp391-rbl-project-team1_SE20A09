-- ══════════════════════════════════════════════════════════════════════════
-- V2.1__create_matchmaking_tables.sql — Matchmaking Module (USP)
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. MatchRequests Table ──────────────────────────────────────────────────
-- Quản lý các kèo ghép đội chơi do Customer tạo ra
CREATE TABLE match_requests (
    match_id            SERIAL PRIMARY KEY,
    user_id             INT             NOT NULL REFERENCES users(user_id),
    stadium_id          INT             NOT NULL REFERENCES stadiums(stadium_id),
    sport_type_id       INT             NOT NULL REFERENCES sport_types(sport_type_id),
    title               VARCHAR(255)    NOT NULL,
    description         TEXT,
    play_date           DATE            NOT NULL,
    start_time          TIME            NOT NULL,
    end_time            TIME            NOT NULL,
    max_players         INT             NOT NULL,
    current_players     INT             NOT NULL DEFAULT 1,
    skill_level         VARCHAR(20)     NOT NULL DEFAULT 'INTERMEDIATE'
                            CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    split_price         BOOLEAN         NOT NULL DEFAULT FALSE,
    price_per_player    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    match_status        VARCHAR(20)     NOT NULL DEFAULT 'OPEN'
                            CHECK (match_status IN ('OPEN', 'FULL', 'CANCELLED', 'COMPLETED')),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 2. JoinRequests Table ───────────────────────────────────────────────────
-- Quản lý các yêu cầu xin tham gia kèo của khách hàng khác
CREATE TABLE join_requests (
    join_id             SERIAL PRIMARY KEY,
    match_id            INT             NOT NULL REFERENCES match_requests(match_id) ON DELETE CASCADE,
    user_id             INT             NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    request_status      VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                            CHECK (request_status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    message             TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ── 3. Indexes ──────────────────────────────────────────────────────────────
-- Tối ưu hóa hiệu năng truy vấn cho luồng tìm kèo và xin tham gia
CREATE INDEX idx_match_requests_user_id     ON match_requests(user_id);
CREATE INDEX idx_match_requests_stadium_id  ON match_requests(stadium_id);
CREATE INDEX idx_match_requests_sport_type  ON match_requests(sport_type_id);
CREATE INDEX idx_match_requests_play_date   ON match_requests(play_date);
CREATE INDEX idx_match_requests_status      ON match_requests(match_status);

CREATE INDEX idx_join_requests_match_id     ON join_requests(match_id);
CREATE INDEX idx_join_requests_user_id      ON join_requests(user_id);
CREATE INDEX idx_join_requests_status       ON join_requests(request_status);
