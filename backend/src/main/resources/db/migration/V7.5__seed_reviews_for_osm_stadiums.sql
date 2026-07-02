-- ══════════════════════════════════════════════════════════════════════════
-- V7.5__seed_reviews_for_osm_stadiums.sql
-- Seed review/rating thực tế (nội dung giả lập tiếng Việt) cho các sân được
-- thêm bởi V7.4 (dữ liệu OSM). Mỗi review đi kèm 1 booking COMPLETED hợp lệ
-- (đúng luồng nghiệp vụ: reviews.booking_id NOT NULL UNIQUE).
--
-- Phạm vi: CHỈ áp dụng cho COURT thuộc complex có cover_image_url bắt đầu
-- bằng 'https://images.pexels.com' — đây là dấu hiệu duy nhất phân biệt các
-- complex được sinh bởi V7.4 với 6 complex cũ đã có review từ trước (V2,
-- V2.2, V4_8, V6). Không đụng tới dữ liệu review/rating đã có sẵn.
--
-- ~80% số sân con (COURT) đủ điều kiện sẽ nhận 1-4 review từ các khách hàng
-- test có sẵn (customer@ .. customer5@sportvenue.com); phần còn lại để trống
-- (giống sân mới chưa ai đánh giá — thực tế hơn là review 100% sân).
-- ══════════════════════════════════════════════════════════════════════════

WITH customers AS (
    SELECT user_id
    FROM users
    WHERE email IN (
        'customer@sportvenue.com', 'customer2@sportvenue.com', 'customer3@sportvenue.com',
        'customer4@sportvenue.com', 'customer5@sportvenue.com'
    )
),
eligible_courts AS (
    SELECT s.stadium_id, s.price_per_hour,
           1 + floor(random() * 4)::int AS review_target
    FROM stadiums s
    JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
    WHERE s.node_type = 'COURT'
      AND sc.cover_image_url LIKE 'https://images.pexels.com%'
      AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.stadium_id = s.stadium_id)
      AND random() < 0.8
),
shuffled AS (
    SELECT ec.stadium_id, ec.price_per_hour, ec.review_target, c.user_id,
           row_number() OVER (PARTITION BY ec.stadium_id ORDER BY random()) AS rn
    FROM eligible_courts ec
    CROSS JOIN customers c
),
selected_pairs AS (
    SELECT stadium_id, price_per_hour, user_id
    FROM shuffled
    WHERE rn <= review_target
),
-- "random() AS r" phải nằm ở 1 CTE RIÊNG, tách khỏi bất kỳ "ORDER BY random()"
-- nào (kể cả trong window function) — nếu 2 lời gọi random() giống hệt nhau
-- về mặt cú pháp xuất hiện trong cùng 1 target list, Postgres có thể coi đó
-- là "cùng 1 biểu thức" và CHỈ TÍNH MỘT LẦN, khiến rating bị lệch theo thứ tự
-- shuffle (đã tự kiểm chứng bug này bằng cách chạy thử trên Postgres thật).
rated_raw AS (
    SELECT stadium_id, price_per_hour, user_id, random() AS r
    FROM selected_pairs
),
rated AS (
    SELECT stadium_id, price_per_hour, user_id,
           CASE
               WHEN r < 0.40 THEN 5
               WHEN r < 0.75 THEN 4
               WHEN r < 0.90 THEN 3
               WHEN r < 0.97 THEN 2
               ELSE 1
           END AS rating_score,
           (CURRENT_DATE - (7 + floor(random() * 150))::int) AS reservation_date
    FROM rated_raw
),
slot_pick AS (
    SELECT rt.*,
           (SELECT t.slot_id FROM time_slots t WHERE t.stadium_id = rt.stadium_id ORDER BY random() LIMIT 1) AS slot_id
    FROM rated rt
),
new_bookings AS (
    INSERT INTO bookings (user_id, stadium_id, slot_id, total_price, booking_status, payment_status, reservation_date, booking_date, note)
    SELECT user_id, stadium_id, slot_id, price_per_hour, 'COMPLETED', 'PAID',
           reservation_date, reservation_date::timestamp, 'Seed review data (OSM dataset)'
    FROM slot_pick
    WHERE slot_id IS NOT NULL
    RETURNING booking_id, user_id, stadium_id
),
comment_pool (rating_score, comment) AS (
    VALUES
        (5, 'Sân đẹp, sạch sẽ, chủ sân nhiệt tình!'),
        (5, 'Chất lượng tuyệt vời, chắc chắn sẽ quay lại.'),
        (5, 'Giá cả hợp lý, đặt sân qua app rất tiện.'),
        (5, 'Ánh sáng tốt, mặt sân mới, chơi rất đã.'),
        (5, 'Nhân viên thân thiện, hỗ trợ nhanh chóng.'),
        (5, 'Vị trí thuận tiện, bãi đỗ xe rộng rãi.'),
        (5, 'Đặt sân nhanh gọn, không gặp trục trặc gì.'),
        (5, 'Sân đúng như mô tả, rất hài lòng.'),
        (5, 'Không gian thoáng mát, phù hợp chơi buổi tối.'),
        (5, 'Dịch vụ chuyên nghiệp, sẽ giới thiệu cho bạn bè.'),
        (4, 'Sân ổn, hơi xa trung tâm nhưng bù lại giá tốt.'),
        (4, 'Chất lượng tốt, chỉ tiếc nhà vệ sinh hơi cũ.'),
        (4, 'Đặt sân dễ dàng, sân đẹp, sẽ quay lại lần sau.'),
        (4, 'Nhìn chung hài lòng, mong cải thiện thêm chỗ gửi xe.'),
        (4, 'Sân tốt nhưng giờ cao điểm hơi đông.'),
        (4, 'Giá hợp lý, chất lượng tương xứng.'),
        (4, 'Sân sạch, chỉ hơi thiếu ánh sáng vào buổi tối.'),
        (4, 'Trải nghiệm khá tốt, sẽ giới thiệu thêm cho bạn bè.'),
        (3, 'Tạm được, cần cải thiện thêm về ánh sáng buổi tối.'),
        (3, 'Sân hơi nhỏ so với mô tả, giá bình thường.'),
        (3, 'Bãi đỗ xe chật, còn lại thì ổn.'),
        (3, 'Chất lượng trung bình, chưa có gì nổi bật.'),
        (3, 'Đặt sân hơi mất thời gian chờ xác nhận.'),
        (3, 'Sân được, nhưng nhân viên phục vụ chưa nhiệt tình lắm.'),
        (2, 'Sân xuống cấp, cần bảo trì sớm.'),
        (2, 'Chờ đợi khá lâu mới được nhận sân.'),
        (2, 'Vệ sinh chưa tốt, cần cải thiện.'),
        (2, 'Giá hơi cao so với chất lượng thực tế.'),
        (2, 'Đặt sân qua app nhưng đến nơi bị đổi giờ đột xuất.'),
        (1, 'Trải nghiệm không tốt, sẽ không quay lại.'),
        (1, 'Sân bẩn, dịch vụ kém.'),
        (1, 'Nhân viên thái độ không tốt.'),
        (1, 'Đặt sân rồi nhưng đến nơi sân đã có người khác dùng.')
),
new_reviews AS (
    INSERT INTO reviews (booking_id, user_id, stadium_id, rating_score, comment, created_at)
    SELECT nb.booking_id, nb.user_id, nb.stadium_id, rt.rating_score,
           (SELECT cp.comment FROM comment_pool cp WHERE cp.rating_score = rt.rating_score ORDER BY random() LIMIT 1),
           rt.reservation_date::timestamp + (1 + floor(random() * 4)) * interval '1 day'
    FROM new_bookings nb
    JOIN rated rt ON rt.stadium_id = nb.stadium_id AND rt.user_id = nb.user_id
    RETURNING review_id, stadium_id, rating_score
),
court_agg AS (
    SELECT stadium_id, COUNT(*) AS cnt, ROUND(AVG(rating_score)::numeric, 2) AS avg_rating
    FROM new_reviews
    GROUP BY stadium_id
),
update_courts AS (
    UPDATE stadiums s
    SET review_count = ca.cnt, average_rating = ca.avg_rating
    FROM court_agg ca
    WHERE s.stadium_id = ca.stadium_id
    RETURNING s.stadium_id, s.parent_stadium_id, s.complex_id
),
facility_agg AS (
    SELECT uc.parent_stadium_id AS facility_id,
           SUM(ca.cnt) AS cnt,
           ROUND((SUM(ca.cnt * ca.avg_rating) / SUM(ca.cnt))::numeric, 2) AS avg_rating
    FROM update_courts uc
    JOIN court_agg ca ON ca.stadium_id = uc.stadium_id
    GROUP BY uc.parent_stadium_id
),
update_facilities AS (
    UPDATE stadiums f
    SET review_count = fa.cnt, average_rating = fa.avg_rating
    FROM facility_agg fa
    WHERE f.stadium_id = fa.facility_id
    RETURNING f.stadium_id
),
complex_agg AS (
    SELECT uc.complex_id,
           SUM(ca.cnt) AS cnt,
           ROUND((SUM(ca.cnt * ca.avg_rating) / SUM(ca.cnt))::numeric, 2) AS avg_rating
    FROM update_courts uc
    JOIN court_agg ca ON ca.stadium_id = uc.stadium_id
    GROUP BY uc.complex_id
)
UPDATE stadium_complexes sc
SET review_count = cxa.cnt, average_rating = cxa.avg_rating
FROM complex_agg cxa
WHERE sc.complex_id = cxa.complex_id;
