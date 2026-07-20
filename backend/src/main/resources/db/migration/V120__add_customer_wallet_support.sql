ALTER TABLE wallets ADD COLUMN user_id INT UNIQUE REFERENCES users(user_id);

ALTER TABLE wallets DROP CONSTRAINT chk_wallet_type;
ALTER TABLE wallets ADD CONSTRAINT chk_wallet_type CHECK (
    (CASE WHEN owner_id IS NOT NULL THEN 1 ELSE 0 END)
  + (CASE WHEN user_id  IS NOT NULL THEN 1 ELSE 0 END)
  + (CASE WHEN is_platform THEN 1 ELSE 0 END) = 1
);

ALTER TABLE payments DROP CONSTRAINT payments_payment_method_check;
ALTER TABLE payments ADD CONSTRAINT payments_payment_method_check
    CHECK (payment_method IN ('CASH', 'VNPAY', 'MOMO', 'BANKING', 'WALLET'));

CREATE TABLE wallet_topups (
    topup_id         SERIAL PRIMARY KEY,
    user_id          INT NOT NULL REFERENCES users(user_id),
    amount           DECIMAL(12,2) NOT NULL,
    transaction_code VARCHAR(100) UNIQUE NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    paid_at          TIMESTAMP
);
CREATE INDEX ix_wallet_topups_user ON wallet_topups(user_id, created_at DESC);
