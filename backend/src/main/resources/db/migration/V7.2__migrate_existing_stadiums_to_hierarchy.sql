-- ══════════════════════════════════════════════════════════════════════════
-- V7.2__migrate_existing_stadiums_to_hierarchy.sql — Di trú dữ liệu an toàn
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Thêm cột tạm và tạo Complex tương ứng với từng Stadium cũ ──────────────
ALTER TABLE stadium_complexes ADD COLUMN temp_source_stadium_id INT;

INSERT INTO stadium_complexes (
    owner_id, name, address, phone, latitude, longitude,
    cover_image_url, complex_status, approved_status, average_rating, review_count, created_at, temp_source_stadium_id
)
SELECT 
    owner_id, 
    stadium_name || ' Complex', 
    address, 
    NULL, -- phone
    latitude, 
    longitude,
    NULL, -- cover_image_url
    stadium_status, 
    approved_status, 
    average_rating, 
    review_count, 
    created_at, 
    stadium_id
FROM stadiums
WHERE node_type = 'COURT' AND complex_id IS NULL;

-- ── 2. Tạo FACILITY tương ứng trỏ về Complex bằng cột tạm ───────────────────
INSERT INTO stadiums (
    owner_id, sport_type_id, stadium_name, description, address,
    node_type, complex_id, parent_stadium_id,
    open_time, close_time, price_per_hour, stadium_status, approved_status,
    average_rating, review_count, created_at
)
SELECT
    s.owner_id, 
    s.sport_type_id, 
    s.stadium_name || ' Facility', 
    s.description, 
    NULL, -- address (thừa hưởng từ complex)
    'FACILITY', 
    sc.complex_id, 
    NULL, -- parent_stadium_id
    s.open_time, 
    s.close_time, 
    s.price_per_hour, 
    s.stadium_status, 
    s.approved_status, -- FACILITY thừa hưởng trạng thái duyệt
    5.0, 
    0, 
    s.created_at
FROM stadiums s
JOIN stadium_complexes sc ON sc.temp_source_stadium_id = s.stadium_id
WHERE s.node_type = 'COURT' AND s.parent_stadium_id IS NULL;

-- ── 3. Cập nhật Stadium cũ thành COURT, trỏ về parent FACILITY bằng cột tạm ────
UPDATE stadiums court
SET node_type = 'COURT',
    complex_id = facility.complex_id,
    parent_stadium_id = facility.stadium_id,
    address = NULL
FROM stadiums facility
JOIN stadium_complexes sc ON sc.complex_id = facility.complex_id
WHERE court.node_type = 'COURT' 
  AND court.parent_stadium_id IS NULL
  AND facility.node_type = 'FACILITY'
  AND sc.temp_source_stadium_id = court.stadium_id;

-- ── 4. Sao chép ảnh cũ sang Complex Images (Tập trung ảnh lên cấp Tổ hợp) ──────
INSERT INTO stadium_complex_images (complex_id, image_url, uploaded_at)
SELECT DISTINCT ON (s.complex_id) s.complex_id, si.image_url, si.uploaded_at
FROM stadium_images si
JOIN stadiums s ON s.stadium_id = si.stadium_id
WHERE s.node_type = 'COURT'
ORDER BY s.complex_id, si.uploaded_at ASC;

-- ── 5. Sao chép Tiện nghi cũ sang Complex Amenities (Tiện nghi cấp Tổ hợp) ──────
INSERT INTO complex_amenities (complex_id, amenity_id)
SELECT DISTINCT s.complex_id, sa.amenity_id
FROM stadium_amenities sa
JOIN stadiums s ON s.stadium_id = sa.stadium_id
WHERE s.node_type = 'COURT'
ON CONFLICT DO NOTHING;

-- ── 6. Điền thông tin môn thể thao liên kết & dọn dẹp cột tạm ─────────────────
INSERT INTO complex_sport_types (complex_id, sport_type_id)
SELECT DISTINCT complex_id, sport_type_id
FROM stadiums
WHERE node_type = 'FACILITY' AND sport_type_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- Dọn dẹp cột tạm an toàn sau khi hoàn tất ánh xạ
ALTER TABLE stadium_complexes DROP COLUMN temp_source_stadium_id;

-- ── 7. Cập nhật các bảng liên quan (match_requests) ───────────────────────────
UPDATE match_requests m
SET complex_id = c.complex_id,
    preferred_court_id = m.stadium_id,
    preferred_facility_id = c.parent_stadium_id
FROM stadiums c
WHERE m.stadium_id = c.stadium_id
  AND c.node_type = 'COURT';

-- ── 8. Cập nhật Trigger để kiểm tra nghiêm ngặt (buộc COURT có parent_stadium_id) 
CREATE OR REPLACE FUNCTION check_stadium_parent_node_type()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.node_type = 'COURT' THEN
        IF NEW.parent_stadium_id IS NULL THEN
            RAISE EXCEPTION 'Một COURT bắt buộc phải có parent_stadium_id trỏ về FACILITY';
        END IF;
        IF (SELECT node_type FROM stadiums WHERE stadium_id = NEW.parent_stadium_id) != 'FACILITY' THEN
            RAISE EXCEPTION 'Parent của COURT phải là một bản ghi loại FACILITY';
        END IF;
    END IF;
    IF NEW.node_type = 'FACILITY' THEN
        IF NEW.parent_stadium_id IS NOT NULL THEN
            RAISE EXCEPTION 'Bản ghi FACILITY không được phép có parent_stadium_id';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ── 9. Kích hoạt kiểm tra toàn vẹn CHECK Constraint của V7.1 ───────────────────
ALTER TABLE stadiums VALIDATE CONSTRAINT chk_stadium_hierarchy;
