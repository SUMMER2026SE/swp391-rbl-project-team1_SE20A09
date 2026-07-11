-- Lưu mã giao dịch thật của VNPay (vnp_TransactionNo) và thời điểm VNPay ghi nhận thanh toán
-- (vnp_PayDate, raw string yyyyMMddHHmmss) — cần thiết cho Refund/QueryDR API thật sau này.
ALTER TABLE payments ADD COLUMN gateway_transaction_no VARCHAR(50);
ALTER TABLE payments ADD COLUMN gateway_pay_date VARCHAR(14);
