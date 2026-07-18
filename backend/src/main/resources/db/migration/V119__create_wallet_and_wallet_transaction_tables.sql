CREATE TABLE wallets (
    wallet_id   SERIAL PRIMARY KEY,
    owner_id    INT UNIQUE REFERENCES owners(owner_id),
    is_platform BOOLEAN NOT NULL DEFAULT FALSE,
    balance     DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_wallet_type CHECK (
        (owner_id IS NOT NULL AND is_platform = FALSE) OR 
        (owner_id IS NULL AND is_platform = TRUE)
    )
);
CREATE UNIQUE INDEX ux_wallets_platform ON wallets(is_platform) WHERE is_platform = TRUE;
INSERT INTO wallets (is_platform, balance) VALUES (TRUE, 0);

CREATE TABLE wallet_transactions (
    transaction_id   SERIAL PRIMARY KEY,
    wallet_id        INT NOT NULL REFERENCES wallets(wallet_id),
    amount           DECIMAL(12,2) NOT NULL,
    booking_id       INT REFERENCES bookings(booking_id),
    note             VARCHAR(255),
    transaction_type VARCHAR(30) NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX ix_wallet_tx_wallet_created ON wallet_transactions(wallet_id, created_at DESC);
