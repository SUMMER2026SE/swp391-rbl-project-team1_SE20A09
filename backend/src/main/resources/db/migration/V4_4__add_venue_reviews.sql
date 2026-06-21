CREATE TABLE venue_reviews (
    id SERIAL PRIMARY KEY,
    venue_id INT NOT NULL,
    customer_id INT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_venue_reviews_stadium FOREIGN KEY (venue_id) REFERENCES stadiums(stadium_id),
    CONSTRAINT fk_venue_reviews_user FOREIGN KEY (customer_id) REFERENCES users(user_id),
    CONSTRAINT uk_venue_customer_review UNIQUE (venue_id, customer_id)
);

ALTER TABLE stadiums
ADD COLUMN review_count INT DEFAULT 0 NOT NULL;
