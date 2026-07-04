-- ══════════════════════════════════════════════════════════════════════════
-- V7.8__seed_accessories_and_amenities_for_osm_stadiums.sql
-- Bổ sung phụ kiện thuê (accessories) cho từng COURT và tiện nghi (amenities)
-- cho từng Complex thuộc bộ dữ liệu OSM (V7.6/V7.7) — hiện đang trống hoàn
-- toàn 2 loại dữ liệu này, làm giảm độ thật của venue detail page.
--
-- Phạm vi: CHỈ áp dụng cho complex có cover_image_url bắt đầu bằng
-- 'https://images.pexels.com' (dấu hiệu riêng của batch V7.6), và chỉ những
-- COURT/complex CHƯA có accessory/amenity nào — không đụng dữ liệu có sẵn.
--
-- Accessories gắn ở cấp COURT (không phải FACILITY) vì đó là đơn vị khách
-- hàng thực sự đặt/xem chi tiết (giống time_slots), khớp với cách
-- AccessoryServiceImpl.getAccessoriesByStadium(stadiumId) đang query.
-- Amenities gắn ở cấp Complex (bảng complex_amenities) — khớp với cách V7.2
-- đã di trú dữ liệu tiện nghi cũ lên cấp Complex.
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Phụ kiện thuê theo từng COURT, phù hợp môn thể thao ─────────────────
WITH eligible_courts AS (
    SELECT s.stadium_id, sp.sport_name,
           2 + floor(random() * 2)::int AS accessory_target  -- mỗi sân có 2-3 loại phụ kiện
    FROM stadiums s
    JOIN stadiums f ON f.stadium_id = s.parent_stadium_id
    JOIN sport_types sp ON sp.sport_type_id = f.sport_type_id
    JOIN stadium_complexes sc ON sc.complex_id = s.complex_id
    WHERE s.node_type = 'COURT'
      AND sc.cover_image_url LIKE 'https://images.pexels.com%'
      AND NOT EXISTS (SELECT 1 FROM accessories a WHERE a.stadium_id = s.stadium_id)
),
accessory_catalog (sport_name, item_name, unit_price) AS (
    VALUES
        ('Football', 'Áo bib', 30000),
        ('Football', 'Bóng đá dự phòng', 50000),
        ('Football', 'Giày đá bóng (thuê)', 40000),
        ('Football', 'Nước uống', 10000),
        ('Badminton', 'Vợt cầu lông (thuê)', 30000),
        ('Badminton', 'Cầu lông (ống 12 quả)', 85000),
        ('Badminton', 'Giày cầu lông (thuê)', 35000),
        ('Badminton', 'Quấn cán vợt', 15000),
        ('Basketball', 'Bóng rổ (thuê)', 30000),
        ('Basketball', 'Áo thi đấu', 25000),
        ('Basketball', 'Băng cổ tay', 15000),
        ('Tennis', 'Vợt tennis (thuê)', 50000),
        ('Tennis', 'Bóng tennis (hộp 3 quả)', 60000),
        ('Tennis', 'Quấn cán vợt', 15000),
        ('Volleyball', 'Bóng chuyền (thuê)', 30000),
        ('Volleyball', 'Lưới dự phòng', 100000),
        ('Volleyball', 'Băng gối', 20000)
),
-- Shuffle & chọn N loại phụ kiện mỗi sân (window ORDER BY random() nằm RIÊNG
-- ở đây, KHÔNG được thêm cột random() phẳng nào khác cùng cấp — bài học từ
-- V7.7: 2 lời gọi random() giống hệt cú pháp trong cùng target list có thể
-- bị Postgres coi là 1 biểu thức, chỉ tính 1 lần).
candidate_pairs AS (
    SELECT ec.stadium_id, ec.accessory_target, ac.item_name, ac.unit_price,
           row_number() OVER (PARTITION BY ec.stadium_id ORDER BY random()) AS rn
    FROM eligible_courts ec
    JOIN accessory_catalog ac ON ac.sport_name = ec.sport_name
),
selected AS (
    SELECT stadium_id, item_name, unit_price
    FROM candidate_pairs
    WHERE rn <= accessory_target
),
-- quantity/is_available cần random() độc lập — tính ở CTE riêng, tách khỏi
-- CTE có window ORDER BY random() phía trên (đúng bài học đã rút ra).
priced AS (
    SELECT stadium_id, item_name, unit_price,
           (3 + floor(random() * 13))::int AS quantity,
           (random() < 0.92) AS is_available
    FROM selected
)
INSERT INTO accessories (stadium_id, name, price_per_unit, quantity, is_available)
SELECT stadium_id, item_name, unit_price, quantity, is_available
FROM priced;

-- ── 2. Tiện nghi theo từng Complex ──────────────────────────────────────────
WITH eligible_complexes AS (
    SELECT sc.complex_id,
           2 + floor(random() * 3)::int AS amenity_target  -- mỗi complex có 2-4 tiện nghi
    FROM stadium_complexes sc
    WHERE sc.cover_image_url LIKE 'https://images.pexels.com%'
      AND NOT EXISTS (SELECT 1 FROM complex_amenities ca WHERE ca.complex_id = sc.complex_id)
),
candidate_pairs AS (
    SELECT ec.complex_id, ec.amenity_target, a.amenity_id,
           row_number() OVER (PARTITION BY ec.complex_id ORDER BY random()) AS rn
    FROM eligible_complexes ec
    CROSS JOIN amenities a
)
INSERT INTO complex_amenities (complex_id, amenity_id)
SELECT complex_id, amenity_id
FROM candidate_pairs
WHERE rn <= amenity_target;
