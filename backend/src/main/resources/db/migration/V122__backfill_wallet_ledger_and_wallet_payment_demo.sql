-- ══════════════════════════════════════════════════════════════════════════
-- V122__backfill_wallet_ledger_and_wallet_payment_demo.sql
--
-- Bảng wallets/wallet_transactions (V119/V120) ra đời SAU khi toàn bộ dữ liệu
-- demo ở V115-V118 đã được viết, nên tất cả booking COMPLETED/CASH-confirmed/
-- CANCELLED cũ đó chưa từng có bút toán ví nào — mở /owner/wallet hay
-- /admin/wallet lên sẽ thấy trống trơn dù đã có hàng chục booking đã thanh
-- toán/hoàn tiền. Migration này backfill lại đúng logic ví hiện hành
-- (BookingServiceImpl/PaymentServiceImpl) cho toàn bộ lịch sử đó, cộng thêm:
--   1. Ví 2 Owner + 5 Customer được khởi tạo tường minh (thay vì chờ lazy-create).
--   2. Nạp sẵn tiền vào ví 2 Customer (customer, customer2) để demo Track 2 có
--      thể thanh toán/pay-remaining bằng Ví ngay mà không cần đợi VNPay sandbox
--      redirect. Cố tình bỏ trống customer5 để dùng demo luồng nạp tiền thật.
--   3. 2 booking mới thanh toán bằng Ví (FULL + DEPOSIT còn nợ phần còn lại)
--      để Track 2 demo trực tiếp payWithWallet/payRemainingWithWallet.
--   4. Cuối cùng recompute lại toàn bộ wallets.balance từ chính lịch sử
--      wallet_transactions vừa backfill — tránh sai số cộng dồn thủ công.
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Khởi tạo ví cho 2 Owner + 5 Customer (ví Platform đã có từ V119) ──
INSERT INTO wallets (owner_id, is_platform, balance)
SELECT owner_id, FALSE, 0 FROM owners WHERE business_name IN ('Sport Venue Owner Corp', 'Huy Sport Center')
ON CONFLICT (owner_id) DO NOTHING;

INSERT INTO wallets (user_id, is_platform, balance)
SELECT user_id, FALSE, 0 FROM users
WHERE email IN ('customer@sportvenue.com', 'customer2@sportvenue.com', 'customer3@sportvenue.com', 'customer4@sportvenue.com', 'customer5@sportvenue.com')
ON CONFLICT (user_id) DO NOTHING;

-- ── 2. Backfill doanh thu cho các payment VNPAY đã SUCCESS (owner net đã trừ
-- phí + platform nhận phí), gồm cả DEMO-25..29 vừa thêm (chưa huỷ, vẫn PAID) ──
INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT ow.wallet_id, p.amount - COALESCE(b.service_fee, 0), b.booking_id,
    'Doanh thu đặt sân #' || b.booking_id || ' (backfill VNPay)', 'BOOKING_CREDIT', p.paid_at
FROM payments p
JOIN bookings b ON b.booking_id = p.booking_id
JOIN stadiums s ON s.stadium_id = b.stadium_id
JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
JOIN owners o ON o.owner_id = sc.owner_id
JOIN wallets ow ON ow.owner_id = o.owner_id
WHERE p.payment_status = 'SUCCESS' AND p.amount > 0 AND p.payment_method = 'VNPAY';

INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT (SELECT wallet_id FROM wallets WHERE is_platform = TRUE), b.service_fee, b.booking_id,
    'Phí dịch vụ từ đơn đặt sân #' || b.booking_id || ' (backfill VNPay)', 'SERVICE_FEE_CREDIT', p.paid_at
FROM payments p
JOIN bookings b ON b.booking_id = p.booking_id
WHERE p.payment_status = 'SUCCESS' AND p.amount > 0 AND p.payment_method = 'VNPAY' AND COALESCE(b.service_fee, 0) > 0;

-- ── 3. Backfill doanh thu cho các payment CASH đã SUCCESS (owner nhận đủ rồi
-- bị khấu trừ phí riêng, platform nhận phí — đúng 2 bước như recordWalletForCashPayment) ──
INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT ow.wallet_id, p.amount, b.booking_id,
    'Doanh thu đặt sân tiền mặt #' || b.booking_id || ' (backfill)', 'BOOKING_CREDIT', p.paid_at
FROM payments p
JOIN bookings b ON b.booking_id = p.booking_id
JOIN stadiums s ON s.stadium_id = b.stadium_id
JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
JOIN owners o ON o.owner_id = sc.owner_id
JOIN wallets ow ON ow.owner_id = o.owner_id
WHERE p.payment_status = 'SUCCESS' AND p.amount > 0 AND p.payment_method = 'CASH';

INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT ow.wallet_id, -b.service_fee, b.booking_id,
    'Khấu trừ phí dịch vụ đơn tiền mặt #' || b.booking_id || ' (backfill)', 'SERVICE_FEE_DEBIT', p.paid_at
FROM payments p
JOIN bookings b ON b.booking_id = p.booking_id
JOIN stadiums s ON s.stadium_id = b.stadium_id
JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
JOIN owners o ON o.owner_id = sc.owner_id
JOIN wallets ow ON ow.owner_id = o.owner_id
WHERE p.payment_status = 'SUCCESS' AND p.amount > 0 AND p.payment_method = 'CASH' AND COALESCE(b.service_fee, 0) > 0;

INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT (SELECT wallet_id FROM wallets WHERE is_platform = TRUE), b.service_fee, b.booking_id,
    'Phí dịch vụ từ đơn tiền mặt #' || b.booking_id || ' (backfill)', 'SERVICE_FEE_CREDIT', p.paid_at
FROM payments p
JOIN bookings b ON b.booking_id = p.booking_id
WHERE p.payment_status = 'SUCCESS' AND p.amount > 0 AND p.payment_method = 'CASH' AND COALESCE(b.service_fee, 0) > 0;

-- ── 4. Backfill phía hoàn tiền cho các booking đã CANCELLED/REFUNDED có tiền
-- hoàn > 0 (DEMO-18,19,20,21: hoàn thường 100%/50%; DEMO-24: OWNER_FAULT;
-- DEMO-30: refund-exception đã được Admin duyệt 50%). Bỏ qua DEMO-22/23/31 vì
-- hoàn 0% — đúng theo guard "if (refundAmount > 0)" của code thật. ──
INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT ow.wallet_id, -(b.total_price - b.service_fee), b.booking_id,
    'Khách huỷ đặt sân tự động (Tiền hoàn đã khấu trừ phí dịch vụ) (backfill)', 'REFUND_DEBIT', b.booking_date + INTERVAL '1 hour'
FROM bookings b JOIN stadiums s ON s.stadium_id = b.stadium_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
JOIN owners o ON o.owner_id = sc.owner_id JOIN wallets ow ON ow.owner_id = o.owner_id
WHERE b.note LIKE '[DEMO-18]%' OR b.note LIKE '[DEMO-19]%';

INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT cw.wallet_id, (b.total_price - b.service_fee), b.booking_id,
    'Hoàn tiền huỷ đơn đặt sân #' || b.booking_id || ' (backfill)', 'CUSTOMER_REFUND_CREDIT', b.booking_date + INTERVAL '1 hour'
FROM bookings b JOIN wallets cw ON cw.user_id = b.user_id
WHERE b.note LIKE '[DEMO-18]%' OR b.note LIKE '[DEMO-19]%';

INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT ow.wallet_id, -((b.total_price - b.service_fee) * 0.5), b.booking_id,
    'Khách huỷ đặt sân tự động (Tiền hoàn đã khấu trừ phí dịch vụ) (backfill)', 'REFUND_DEBIT', b.booking_date + INTERVAL '1 hour'
FROM bookings b JOIN stadiums s ON s.stadium_id = b.stadium_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
JOIN owners o ON o.owner_id = sc.owner_id JOIN wallets ow ON ow.owner_id = o.owner_id
WHERE b.note LIKE '[DEMO-20]%' OR b.note LIKE '[DEMO-21]%';

INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT cw.wallet_id, ((b.total_price - b.service_fee) * 0.5), b.booking_id,
    'Hoàn tiền huỷ đơn đặt sân #' || b.booking_id || ' (backfill)', 'CUSTOMER_REFUND_CREDIT', b.booking_date + INTERVAL '1 hour'
FROM bookings b JOIN wallets cw ON cw.user_id = b.user_id
WHERE b.note LIKE '[DEMO-20]%' OR b.note LIKE '[DEMO-21]%';

-- DEMO-24: OWNER_FAULT — Owner mất full (trừ phí), Platform hoàn lại phí, Customer nhận đủ 100%
INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT ow.wallet_id, -(b.total_price - b.service_fee), b.booking_id,
    'Khách hoàn tiền do lỗi chủ sân (Owner Fault) (backfill)', 'REFUND_DEBIT', b.booking_date + INTERVAL '1 hour'
FROM bookings b JOIN stadiums s ON s.stadium_id = b.stadium_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
JOIN owners o ON o.owner_id = sc.owner_id JOIN wallets ow ON ow.owner_id = o.owner_id
WHERE b.note LIKE '[DEMO-24]%';

INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT (SELECT wallet_id FROM wallets WHERE is_platform = TRUE), -b.service_fee, b.booking_id,
    'Platform hoàn lại phí dịch vụ đơn #' || b.booking_id || ' (backfill)', 'REFUND_FEE_DEBIT', b.booking_date + INTERVAL '1 hour'
FROM bookings b WHERE b.note LIKE '[DEMO-24]%';

INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT cw.wallet_id, b.total_price, b.booking_id,
    'Hoàn tiền huỷ đơn đặt sân #' || b.booking_id || ' (backfill)', 'CUSTOMER_REFUND_CREDIT', b.booking_date + INTERVAL '1 hour'
FROM bookings b JOIN wallets cw ON cw.user_id = b.user_id
WHERE b.note LIKE '[DEMO-24]%';

-- DEMO-30: Refund Exception Admin duyệt 50% (FORCE_MAJEURE luôn giữ lại phí dịch vụ)
INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT ow.wallet_id, -((b.total_price - b.service_fee) * 0.5), b.booking_id,
    'Hoàn tiền do sự kiện bất khả kháng (Force Majeure) (backfill)', 'REFUND_DEBIT', NOW() - INTERVAL '26 hours'
FROM bookings b JOIN stadiums s ON s.stadium_id = b.stadium_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
JOIN owners o ON o.owner_id = sc.owner_id JOIN wallets ow ON ow.owner_id = o.owner_id
WHERE b.note LIKE '[DEMO-30]%';

INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT cw.wallet_id, ((b.total_price - b.service_fee) * 0.5), b.booking_id,
    'Hoàn tiền ngoại lệ bất khả kháng đơn đặt sân #' || b.booking_id || ' (backfill)', 'CUSTOMER_REFUND_CREDIT', NOW() - INTERVAL '26 hours'
FROM bookings b JOIN wallets cw ON cw.user_id = b.user_id
WHERE b.note LIKE '[DEMO-30]%';

-- ── 5. Pre-fund ví Customer để demo Track 2 thanh toán bằng Ví ngay không cần
-- chờ VNPay sandbox — customer5 cố tình để trống 0đ để demo luồng nạp tiền thật ──
INSERT INTO wallet_topups (user_id, amount, transaction_code, status, created_at, paid_at)
VALUES
    ((SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'), 3000000.00, 'TOPUP-DEMO-CUST1', 'SUCCESS', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '3 minutes'),
    ((SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'), 1500000.00, 'TOPUP-DEMO-CUST2', 'SUCCESS', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '3 minutes');

INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT cw.wallet_id, 3000000.00, NULL, 'Nạp tiền vào ví qua VNPay (backfill demo)', 'CUSTOMER_TOPUP_CREDIT', NOW() - INTERVAL '2 days' + INTERVAL '3 minutes'
FROM wallets cw WHERE cw.user_id = (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com');

INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
SELECT cw.wallet_id, 1500000.00, NULL, 'Nạp tiền vào ví qua VNPay (backfill demo)', 'CUSTOMER_TOPUP_CREDIT', NOW() - INTERVAL '2 days' + INTERVAL '3 minutes'
FROM wallets cw WHERE cw.user_id = (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com');

-- ── 6. DEMO-32: thanh toán FULL bằng Ví (payWithWallet) ──
DO $$
DECLARE
    v_stadium_id INT;
    v_slot_id INT;
    v_booking_id INT;
    v_total NUMERIC := 100000.00;
    v_fee NUMERIC := 5000.00;
    v_customer_id INT;
    v_owner_id INT;
BEGIN
    SELECT s.stadium_id INTO v_stadium_id FROM stadiums s
    JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
    JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
    JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
    WHERE sc.name = 'Cung Thể thao Tiên Sơn' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 3';

    SELECT slot_id INTO v_slot_id FROM time_slots WHERE stadium_id = v_stadium_id AND start_time = '08:00:00';
    SELECT user_id INTO v_customer_id FROM users WHERE email = 'customer@sportvenue.com';
    SELECT o.owner_id INTO v_owner_id FROM stadiums s JOIN stadium_complexes sc ON sc.complex_id = s.complex_id JOIN owners o ON o.owner_id = sc.owner_id WHERE s.stadium_id = v_stadium_id;

    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note, expired_at)
    VALUES (v_customer_id, v_stadium_id, v_slot_id, v_total, v_fee, 'CONFIRMED', 'PAID', NOW() - INTERVAL '30 minutes', CURRENT_DATE + 3,
        '[DEMO-32] Đã thanh toán toàn phần bằng Ví nội bộ (payWithWallet FULL).', NULL)
    RETURNING booking_id INTO v_booking_id;

    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    VALUES (v_booking_id, 'WALLET', v_total, 'SUCCESS', NOW() - INTERVAL '30 minutes', 'WALLET-' || v_booking_id || '-' || to_char(NOW(), 'YYYYMMDDHH24MISS'));

    INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
    SELECT wallet_id, -v_total, v_booking_id, 'Thanh toán đơn đặt sân #' || v_booking_id || ' bằng Ví', 'CUSTOMER_PAYMENT_DEBIT', NOW() - INTERVAL '30 minutes'
    FROM wallets WHERE user_id = v_customer_id;

    INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
    SELECT wallet_id, v_total - v_fee, v_booking_id, 'Doanh thu đặt sân #' || v_booking_id || ' bằng Ví (đã trừ phí dịch vụ)', 'BOOKING_CREDIT', NOW() - INTERVAL '30 minutes'
    FROM wallets WHERE owner_id = v_owner_id;

    INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
    SELECT wallet_id, v_fee, v_booking_id, 'Phí dịch vụ từ đơn đặt sân #' || v_booking_id, 'SERVICE_FEE_CREDIT', NOW() - INTERVAL '30 minutes'
    FROM wallets WHERE is_platform = TRUE;
END $$;

-- ── 7. DEMO-33: đặt cọc bằng Ví (payWithWallet DEPOSIT), phần còn lại CHƯA trả
-- — sẵn sàng demo payRemainingWithWallet trực tiếp trong buổi demo Track 2 ──
DO $$
DECLARE
    v_stadium_id INT;
    v_slot_id INT;
    v_booking_id INT;
    v_total NUMERIC := 195000.00;
    v_fee NUMERIC := 10000.00;
    v_deposit NUMERIC := 58500.00;
    v_customer_id INT;
    v_owner_id INT;
BEGIN
    SELECT s.stadium_id INTO v_stadium_id FROM stadiums s
    JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id
    JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id
    JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
    WHERE sc.name = 'Sân bóng rổ Quân Khu 5' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 2';

    SELECT slot_id INTO v_slot_id FROM time_slots WHERE stadium_id = v_stadium_id AND start_time = '09:00:00';
    SELECT user_id INTO v_customer_id FROM users WHERE email = 'customer2@sportvenue.com';
    SELECT o.owner_id INTO v_owner_id FROM stadiums s JOIN stadium_complexes sc ON sc.complex_id = s.complex_id JOIN owners o ON o.owner_id = sc.owner_id WHERE s.stadium_id = v_stadium_id;

    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note, expired_at)
    VALUES (v_customer_id, v_stadium_id, v_slot_id, v_total, v_fee, 'CONFIRMED', 'DEPOSITED', NOW() - INTERVAL '20 minutes', CURRENT_DATE + 3,
        '[DEMO-33] Đã đặt cọc bằng Ví (payWithWallet DEPOSIT) — sẵn sàng demo trả nốt bằng Ví (payRemainingWithWallet) trực tiếp.', NULL)
    RETURNING booking_id INTO v_booking_id;

    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    VALUES (v_booking_id, 'WALLET', v_deposit, 'SUCCESS', NOW() - INTERVAL '20 minutes', 'WALLET-' || v_booking_id || '-' || to_char(NOW(), 'YYYYMMDDHH24MISS'));

    INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
    SELECT wallet_id, -v_deposit, v_booking_id, 'Đặt cọc đơn đặt sân #' || v_booking_id || ' bằng Ví', 'CUSTOMER_PAYMENT_DEBIT', NOW() - INTERVAL '20 minutes'
    FROM wallets WHERE user_id = v_customer_id;

    INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
    SELECT wallet_id, v_deposit - v_fee, v_booking_id, 'Tiền đặt cọc đặt sân #' || v_booking_id || ' bằng Ví (đã trừ phí dịch vụ)', 'BOOKING_CREDIT', NOW() - INTERVAL '20 minutes'
    FROM wallets WHERE owner_id = v_owner_id;

    INSERT INTO wallet_transactions (wallet_id, amount, booking_id, note, transaction_type, created_at)
    SELECT wallet_id, v_fee, v_booking_id, 'Phí dịch vụ từ đơn đặt cọc #' || v_booking_id, 'SERVICE_FEE_CREDIT', NOW() - INTERVAL '20 minutes'
    FROM wallets WHERE is_platform = TRUE;
END $$;

-- ── 8. Recompute lại toàn bộ số dư ví từ chính lịch sử wallet_transactions
-- vừa backfill — tránh sai số cộng dồn thủ công qua nhiều bước ở trên ──
UPDATE wallets w
SET balance = COALESCE((SELECT SUM(amount) FROM wallet_transactions wt WHERE wt.wallet_id = w.wallet_id), 0),
    updated_at = NOW();
