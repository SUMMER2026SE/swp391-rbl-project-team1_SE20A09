-- 1. Thêm cột matching_type (mặc định là INDIVIDUAL để không ảnh hưởng dữ liệu cũ)
ALTER TABLE match_requests 
ADD COLUMN matching_type VARCHAR(30) NOT NULL DEFAULT 'INDIVIDUAL';

-- 2. Thêm ràng buộc check constraint để đảm bảo an toàn dữ liệu ở tầng DB
ALTER TABLE match_requests 
ADD CONSTRAINT chk_matching_type CHECK (matching_type IN ('INDIVIDUAL', 'TEAM_VS_TEAM'));
