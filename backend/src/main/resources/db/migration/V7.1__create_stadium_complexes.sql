-- ══════════════════════════════════════════════════════════════════════════
-- V7.1__create_stadium_complexes.sql — Khởi tạo cấu trúc 3 tầng
-- ══════════════════════════════════════════════════════════════════════════

-- ── 1. Tạo bảng stadium_complexes (Tổ hợp) ──────────────────────────────────
CREATE TABLE stadium_complexes (
    complex_id        SERIAL PRIMARY KEY,
    owner_id          INT NOT NULL REFERENCES owners(owner_id),
    name              VARCHAR(150) NOT NULL,
    description       TEXT,
    address           TEXT NOT NULL,
    phone             VARCHAR(20),
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    cover_image_url   VARCHAR(255),
    complex_status    VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (complex_status IN ('AVAILABLE', 'MAINTENANCE', 'CLOSED')),
    approved_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (approved_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejection_reason  VARCHAR(255),
    average_rating    DECIMAL(3,2) NOT NULL DEFAULT 5.0,
    review_count      INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP
);

-- ── 2. Tạo các bảng vệ tinh cho Complex (Ảnh, Tiện nghi, Môn thể thao) ────────
CREATE TABLE stadium_complex_images (
    image_id    SERIAL PRIMARY KEY,
    complex_id  INT NOT NULL REFERENCES stadium_complexes(complex_id) ON DELETE CASCADE,
    image_url   VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE complex_amenities (
    complex_id INT NOT NULL REFERENCES stadium_complexes(complex_id) ON DELETE CASCADE,
    amenity_id INT NOT NULL REFERENCES amenities(amenity_id) ON DELETE CASCADE,
    PRIMARY KEY (complex_id, amenity_id)
);

CREATE TABLE complex_sport_types (
    complex_id    INT NOT NULL REFERENCES stadium_complexes(complex_id) ON DELETE CASCADE,
    sport_type_id INT NOT NULL REFERENCES sport_types(sport_type_id),
    PRIMARY KEY (complex_id, sport_type_id)
);

-- ── 3. Cập nhật bảng stadiums để hỗ trợ phân cấp cây ──────────────────────────
-- Thêm các cột phục vụ cấu trúc cây tự tham chiếu & denormalization
ALTER TABLE stadiums ADD COLUMN node_type VARCHAR(20) NOT NULL DEFAULT 'COURT'
    CHECK (node_type IN ('FACILITY', 'COURT'));

ALTER TABLE stadiums ADD COLUMN complex_id INT
    REFERENCES stadium_complexes(complex_id);

ALTER TABLE stadiums ADD COLUMN parent_stadium_id INT
    REFERENCES stadiums(stadium_id);

-- Nới lỏng các ràng buộc cũ để cho phép thừa kế giá trị từ nút cha
ALTER TABLE stadiums ALTER COLUMN address DROP NOT NULL;
ALTER TABLE stadiums ALTER COLUMN sport_type_id DROP NOT NULL;
ALTER TABLE stadiums ALTER COLUMN owner_id DROP NOT NULL;
ALTER TABLE stadiums ALTER COLUMN approved_status DROP NOT NULL;

-- Tạo ràng buộc kiểm tra cấu trúc cây nhưng KHÔNG validate ngay lập tức (tránh crash với dữ liệu phẳng cũ)
ALTER TABLE stadiums ADD CONSTRAINT chk_stadium_hierarchy CHECK (
    (node_type = 'FACILITY' AND parent_stadium_id IS NULL AND complex_id IS NOT NULL)
    OR
    (node_type = 'COURT' AND parent_stadium_id IS NOT NULL AND complex_id IS NOT NULL)
) NOT VALID;

-- Tạo index tăng tốc độ truy vấn cây
CREATE INDEX idx_stadiums_complex_id ON stadiums(complex_id);
CREATE INDEX idx_stadiums_parent_id ON stadiums(parent_stadium_id);
CREATE INDEX idx_stadiums_node_type ON stadiums(node_type);

-- ── 4. Tạo Trigger ngăn chặn lỗi tạo cây sai định dạng (COURT -> COURT) ────────
CREATE OR REPLACE FUNCTION check_stadium_parent_node_type()
RETURNS TRIGGER AS $$
BEGIN
    -- Chỉ thực hiện check khi trường parent_stadium_id đã được gán (sau hoặc trong di trú)
    IF NEW.node_type = 'COURT' AND NEW.parent_stadium_id IS NOT NULL THEN
        IF (SELECT node_type FROM stadiums WHERE stadium_id = NEW.parent_stadium_id) != 'FACILITY' THEN
            RAISE EXCEPTION 'Parent của COURT bắt buộc phải là một bản ghi loại FACILITY';
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

CREATE TRIGGER tg_check_stadium_parent_node_type
BEFORE INSERT OR UPDATE ON stadiums
FOR EACH ROW EXECUTE FUNCTION check_stadium_parent_node_type();

-- ── 5. Cập nhật bảng match_requests để ghép kèo theo Tổ hợp (Complex) ──────────
ALTER TABLE match_requests ADD COLUMN complex_id INT
    REFERENCES stadium_complexes(complex_id);

ALTER TABLE match_requests ADD COLUMN preferred_facility_id INT
    REFERENCES stadiums(stadium_id);

ALTER TABLE match_requests ADD COLUMN preferred_court_id INT
    REFERENCES stadiums(stadium_id);

ALTER TABLE match_requests ALTER COLUMN stadium_id DROP NOT NULL;
