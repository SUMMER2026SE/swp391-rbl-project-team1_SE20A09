-- ══════════════════════════════════════════════════════════════════════════
-- V117__seed_demo_stadium_images_and_ratings.sql
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Seed realistic sport-specific images into stadium_images ──
-- For all stadiums (facilities and courts) under the 11 new complexes that currently have no images.
WITH stadium_sport AS (
    SELECT 
        s.stadium_id,
        COALESCE(st.sport_name, pst.sport_name) AS sport_name
    FROM stadiums s
    LEFT JOIN sport_types st ON s.sport_type_id = st.sport_type_id
    LEFT JOIN stadiums parent ON s.parent_stadium_id = parent.stadium_id
    LEFT JOIN sport_types pst ON parent.sport_type_id = pst.sport_type_id
)
INSERT INTO stadium_images (stadium_id, image_url)
SELECT ss.stadium_id, unnest(
    CASE ss.sport_name
        WHEN 'Football' THEN ARRAY[
            'https://images.pexels.com/photos/1142948/pexels-photo-1142948.jpeg?auto=compress&cs=tinysrgb&h=650&w=940',
            'https://images.pexels.com/photos/39811/pexels-photo-39811.jpeg?auto=compress&cs=tinysrgb&h=650&w=940',
            'https://images.pexels.com/photos/47730/soccer-football-field-sport-47730.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'
        ]
        WHEN 'Badminton' THEN ARRAY[
            'https://images.pexels.com/photos/3660204/pexels-photo-3660204.jpeg?auto=compress&cs=tinysrgb&h=650&w=940',
            'https://images.pexels.com/photos/8286363/pexels-photo-8286363.jpeg?auto=compress&cs=tinysrgb&h=650&w=940',
            'https://images.pexels.com/photos/8286362/pexels-photo-8286362.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'
        ]
        WHEN 'Basketball' THEN ARRAY[
            'https://images.pexels.com/photos/5275524/pexels-photo-5275524.jpeg?auto=compress&cs=tinysrgb&h=650&w=940',
            'https://images.pexels.com/photos/1752757/pexels-photo-1752757.jpeg?auto=compress&cs=tinysrgb&h=650&w=940',
            'https://images.pexels.com/photos/35647228/pexels-photo-35647228.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'
        ]
        WHEN 'Tennis' THEN ARRAY[
            'https://images.pexels.com/photos/17368514/pexels-photo-17368514.jpeg?auto=compress&cs=tinysrgb&h=650&w=940',
            'https://images.pexels.com/photos/209977/pexels-photo-209977.jpeg?auto=compress&cs=tinysrgb&h=650&w=940',
            'https://images.pexels.com/photos/2912981/pexels-photo-2912981.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'
        ]
        WHEN 'Volleyball' THEN ARRAY[
            'https://images.pexels.com/photos/6203569/pexels-photo-6203569.jpeg?auto=compress&cs=tinysrgb&h=650&w=940',
            'https://images.pexels.com/photos/1263426/pexels-photo-1263426.jpeg?auto=compress&cs=tinysrgb&h=650&w=940',
            'https://images.pexels.com/photos/36382714/pexels-photo-36382714.jpeg?auto=compress&cs=tinysrgb&h=650&w=940'
        ]
        ELSE ARRAY['https://images.pexels.com/photos/5176497/pexels-photo-5176497.jpeg?auto=compress&cs=tinysrgb&h=650&w=940']
    END
)
FROM stadium_sport ss
WHERE NOT EXISTS (
    SELECT 1 FROM stadium_images si WHERE si.stadium_id = ss.stadium_id
);

-- ── 2. Seed diverse COMPLETED bookings, payments, and reviews across 11 complexes ──

-- Extra 01: Cung Thể thao Tiên Sơn / Sân 2 (Badminton) — 5 stars
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Cung Thể thao Tiên Sơn' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 2'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '10:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '12 days', CURRENT_DATE - 12, '[DEMO-EXTRA-01]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '12 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 5, 'Sân rất rộng và thoáng mát, thảm trải sàn xịn, chơi không lo trơn trượt.', NULL, NOW() - INTERVAL '12 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 02: Cung Thể thao Tiên Sơn / Sân 1 (Basketball) — 4 stars
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Cung Thể thao Tiên Sơn' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '17:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '10 days', CURRENT_DATE - 10, '[DEMO-EXTRA-02]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '10 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 4, 'Sân bóng rổ tiêu chuẩn thi đấu, rổ xịn, nảy bóng tốt, điểm trừ là nhà vệ sinh hơi xa.', NULL, NOW() - INTERVAL '10 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 03: Sân vận động Chi Lăng / Sân 1 (Football) — 5 stars
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Sân vận động Chi Lăng' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 1'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '18:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '8 days', CURRENT_DATE - 8, '[DEMO-EXTRA-03]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '8 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 5, 'Cỏ tự nhiên mướt mắt, đá rất êm chân, giá rẻ hơn so với các sân trung tâm.', NULL, NOW() - INTERVAL '8 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 04: Sân vận động Chi Lăng / Sân 2 (Football) — 2 stars (has owner response)
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Sân vận động Chi Lăng' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 2'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer4@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '19:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '6 days', CURRENT_DATE - 6, '[DEMO-EXTRA-04]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '6 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 2, 'Mặt cỏ nhiều chỗ lồi lõm và bị lún bùn sau mưa, đề nghị ban quản lý bảo dưỡng.', 'Cảm ơn đóng góp của bạn, chúng tôi đang cho cải tạo lại mặt sân cỏ trong tuần này.', NOW() - INTERVAL '6 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 05: Sân vận động Thanh Khê / Sân 1 (Football) — 3 stars
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Sân vận động Thanh Khê' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 1'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer5@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '16:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '14 days', CURRENT_DATE - 14, '[DEMO-EXTRA-05]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '14 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 3, 'Chất lượng sân tạm ổn, tuy nhiên hệ thống thoát nước chưa tốt lắm khi trời mưa to.', NULL, NOW() - INTERVAL '14 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 06: Trung tâm Huấn luyện Thể thao Quốc gia Đà Nẵng / Sân 1 (Tennis) — 5 stars
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Trung tâm Huấn luyện Thể thao Quốc gia Đà Nẵng' AND sp.sport_name = 'Tennis' AND s.stadium_name = 'Sân 1'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '09:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '20 days', CURRENT_DATE - 20, '[DEMO-EXTRA-06]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '20 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 5, 'Sân tennis đất nện chất lượng cực tốt, dịch vụ đi kèm chuyên nghiệp.', NULL, NOW() - INTERVAL '20 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 07: Trung tâm Huấn luyện Thể thao Quốc gia Đà Nẵng / Sân 1 (Badminton) — 4 stars
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Trung tâm Huấn luyện Thể thao Quốc gia Đà Nẵng' AND sp.sport_name = 'Badminton' AND s.stadium_name = 'Sân 1'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '15:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '18 days', CURRENT_DATE - 18, '[DEMO-EXTRA-07]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '18 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 4, 'Trần cao thoáng đãng, ánh sáng tốt không chói mắt, giá cả hợp lý.', NULL, NOW() - INTERVAL '18 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 08: Sân bóng rổ Quân Khu 5 / Sân 1 (Basketball) — 5 stars
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Sân bóng rổ Quân Khu 5' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '19:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '15 days', CURRENT_DATE - 15, '[DEMO-EXTRA-08]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '15 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 5, 'Sân bóng rổ ngoài trời có mái che rất tiện lợi, đèn LED siêu sáng thích hợp chơi ban đêm.', NULL, NOW() - INTERVAL '15 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 09: Sân vận động Hàng Đẫy / Sân 1 (Football) — 5 stars
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Sân vận động Hàng Đẫy' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 1'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer4@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '18:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '25 days', CURRENT_DATE - 25, '[DEMO-EXTRA-09]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '25 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 5, 'Được đá trên mặt cỏ Hàng Đẫy là trải nghiệm tuyệt vời cho mọi cầu thủ phong trào.', NULL, NOW() - INTERVAL '25 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 10: Cung thể thao Quần Ngựa / Sân 2 (Basketball) — 4 stars
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Cung thể thao Quần Ngựa' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 2'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer5@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '20:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '22 days', CURRENT_DATE - 22, '[DEMO-EXTRA-10]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '22 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 4, 'Không gian thoáng mát, rổ bóng chuẩn, dịch vụ cho thuê bóng đầy đủ.', NULL, NOW() - INTERVAL '22 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 11: Nhà thi đấu Tương Mai / Sân 1 (Volleyball) — 3 stars (has owner response)
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Nhà thi đấu Tương Mai' AND sp.sport_name = 'Basketball' AND s.stadium_name = 'Sân 1'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '19:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '9 days', CURRENT_DATE - 9, '[DEMO-EXTRA-11]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '9 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 3, 'Sân rộng sạch nhưng trần hơi thấp một chút, đập bóng cao dễ bị chạm trần.', 'Dạ cảm ơn anh, tụi em sẽ lưu ý kiểm tra để điều chỉnh lưới phù hợp hơn.', NOW() - INTERVAL '9 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 12: Sân vận động Thống Nhất / Sân 1 (Football) — 5 stars
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Sân vận động Thống Nhất' AND sp.sport_name = 'Football' AND s.stadium_name = 'Sân 1'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer2@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '17:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '4 days', CURRENT_DATE - 4, '[DEMO-EXTRA-12]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '4 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 5, 'Sân cỏ tự nhiên đẹp nhất nhì thành phố, phòng thay đồ sạch sẽ và hiện đại.', NULL, NOW() - INTERVAL '4 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 13: Nhà thi đấu đa năng Quận 7 / Sân 1 (Tennis) — 4 stars
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Nhà thi đấu đa năng Quận 7' AND sp.sport_name = 'Tennis' AND s.stadium_name = 'Sân 1'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer3@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '16:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '11 days', CURRENT_DATE - 11, '[DEMO-EXTRA-13]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '11 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 4, 'Mặt sân cứng còn mới, độ nảy bóng đều, dịch vụ chu đáo.', NULL, NOW() - INTERVAL '11 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;

-- Extra 14: Nhà thi đấu Quân khu 7 / Sân 1 (Volleyball) — 5 stars
WITH target_stadium AS (
    SELECT s.stadium_id, s.price_per_hour FROM stadiums s JOIN stadiums fac ON fac.stadium_id = s.parent_stadium_id JOIN sport_types sp ON sp.sport_type_id = fac.sport_type_id JOIN stadium_complexes sc ON sc.complex_id = s.complex_id WHERE sc.name = 'Nhà thi đấu Quân khu 7' AND sp.sport_name = 'Volleyball' AND s.stadium_name = 'Sân 1'
), ins_booking AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, service_fee, booking_status, payment_status, booking_date, reservation_date, note)
    SELECT (SELECT user_id FROM users WHERE email = 'customer4@sportvenue.com'), ts.stadium_id, (SELECT slot_id FROM time_slots WHERE stadium_id = ts.stadium_id AND start_time = '15:00:00'), ts.price_per_hour, ROUND(ts.price_per_hour * 0.05, 2), 'COMPLETED', 'PAID', NOW() - INTERVAL '16 days', CURRENT_DATE - 16, '[DEMO-EXTRA-14]' FROM target_stadium ts RETURNING booking_id, total_price
), ins_payment AS (
    INSERT INTO payments (booking_id, payment_method, amount, payment_status, paid_at, transaction_code)
    SELECT booking_id, 'VNPAY', total_price, 'SUCCESS', NOW() - INTERVAL '16 days' + INTERVAL '5 minutes', 'VNP-EXTRA-' || booking_id FROM ins_booking RETURNING booking_id
)
INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, owner_response, created_at)
SELECT b.booking_id, b.user_id, b.stadium_id, 5, 'Sân bóng chuyền trong nhà tiêu chuẩn, mặt sàn gỗ xịn chống trơn trượt rất tốt.', NULL, NOW() - INTERVAL '16 days' + INTERVAL '3 hours' FROM ins_payment ip JOIN bookings b ON b.booking_id = ip.booking_id;


-- ── 3. Recalculate and update all average ratings and review counts ──
-- Retrospectively updates all complexes and stadiums based on the actual reviews populated in DB.

-- Update stadiums (courts)
UPDATE stadiums s
SET 
  average_rating = COALESCE((SELECT ROUND(AVG(rating_score), 2) FROM reviews r WHERE r.stadium_id = s.stadium_id), 5.0),
  review_count = (SELECT COUNT(*) FROM reviews r WHERE r.stadium_id = s.stadium_id);

-- Update stadium complexes
UPDATE stadium_complexes sc
SET 
  average_rating = COALESCE((SELECT ROUND(AVG(r.rating_score), 2) FROM reviews r JOIN stadiums s ON r.stadium_id = s.stadium_id WHERE s.complex_id = sc.complex_id), 5.0),
  review_count = (SELECT COUNT(*) FROM reviews r JOIN stadiums s ON r.stadium_id = s.stadium_id WHERE s.complex_id = sc.complex_id);
