CREATE TABLE time_slot_exceptions (
    exception_id         SERIAL PRIMARY KEY,
    slot_id              INT NOT NULL REFERENCES time_slots(slot_id) ON DELETE CASCADE,
    exception_date       DATE NOT NULL,
    price_override       DECIMAL(10, 2),
    start_time_override  TIME,
    end_time_override    TIME,
    is_closed            BOOLEAN DEFAULT FALSE,
    is_hidden            BOOLEAN DEFAULT FALSE,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_slot_date UNIQUE (slot_id, exception_date)
);

CREATE INDEX idx_slot_exceptions_date ON time_slot_exceptions(exception_date);
