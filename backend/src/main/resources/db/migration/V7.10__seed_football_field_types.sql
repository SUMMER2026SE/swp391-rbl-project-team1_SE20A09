-- V7.10__seed_football_field_types.sql

-- Gán ngẫu nhiên loại sân 5, sân 7, sân 11, sân Futsal cho các sân lẻ thuộc môn bóng đá (sport_type_id của bóng đá thường là 1)
-- Chúng ta gán dựa trên phép chia lấy dư của stadium_id

-- 1. Gán sân 5 người
UPDATE stadiums
SET football_field_type = 'FIVE_A_SIDE'
WHERE node_type = 'COURT' 
  AND sport_type_id = (SELECT sport_type_id FROM sport_types WHERE sport_code = 'FOOTBALL' LIMIT 1)
  AND stadium_id % 4 = 0;

-- 2. Gán sân 7 người
UPDATE stadiums
SET football_field_type = 'SEVEN_A_SIDE'
WHERE node_type = 'COURT' 
  AND sport_type_id = (SELECT sport_type_id FROM sport_types WHERE sport_code = 'FOOTBALL' LIMIT 1)
  AND stadium_id % 4 = 1;

-- 3. Gán sân 11 người
UPDATE stadiums
SET football_field_type = 'ELEVEN_A_SIDE'
WHERE node_type = 'COURT' 
  AND sport_type_id = (SELECT sport_type_id FROM sport_types WHERE sport_code = 'FOOTBALL' LIMIT 1)
  AND stadium_id % 4 = 2;

-- 4. Gán sân Futsal
UPDATE stadiums
SET football_field_type = 'FUTSAL'
WHERE node_type = 'COURT' 
  AND sport_type_id = (SELECT sport_type_id FROM sport_types WHERE sport_code = 'FOOTBALL' LIMIT 1)
  AND stadium_id % 4 = 3;
