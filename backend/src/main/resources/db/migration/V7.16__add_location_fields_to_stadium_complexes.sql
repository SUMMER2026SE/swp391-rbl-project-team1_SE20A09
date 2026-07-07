-- Cột location chuẩn hoá cho StadiumComplex — thay thế dần cho việc LIKE thô trên address
-- tự do (không đồng nhất giữa "Hồ Chí Minh" / "Thành phố Hồ Chí Minh" / "TP.HCM").
-- Backfill dữ liệu cũ được xử lý bằng LocationBackfillRunner (Java) lúc khởi động app,
-- không xử lý trong migration này để tái dùng chung logic alias-matching với ứng dụng.
ALTER TABLE stadium_complexes ADD COLUMN province VARCHAR(100);
ALTER TABLE stadium_complexes ADD COLUMN district VARCHAR(100);

CREATE INDEX idx_stadium_complexes_province ON stadium_complexes(province);
CREATE INDEX idx_stadium_complexes_district ON stadium_complexes(district);
