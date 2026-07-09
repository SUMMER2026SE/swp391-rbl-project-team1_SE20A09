-- V102 cho phép giá trị 'AWAITING_CASH_PAYMENT' (21 ký tự) qua CHECK constraint,
-- nhưng quên nới độ dài cột — payment_status vẫn là VARCHAR(20) nên mọi UPDATE
-- gán giá trị này đều bị Postgres từ chối với "value too long for type character
-- varying(20)". Nới cột để khớp với CHECK constraint đã cho phép.
ALTER TABLE bookings ALTER COLUMN payment_status TYPE VARCHAR(30);
