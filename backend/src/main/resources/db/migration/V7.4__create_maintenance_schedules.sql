CREATE TABLE maintenance_schedules (
    maintenance_id  SERIAL PRIMARY KEY,
    stadium_id      INT NOT NULL REFERENCES stadiums(stadium_id) ON DELETE CASCADE,
    start_date      DATE NOT NULL,
    end_date        DATE,                 -- NULL = vô thời hạn
    reason          VARCHAR(255),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_maintenance_schedules_stadium ON maintenance_schedules(stadium_id);
CREATE INDEX idx_maintenance_schedules_dates ON maintenance_schedules(start_date, end_date);
