-- V8.1__enrich_complex_addresses_from_coords.sql
-- Sinh tự động bởi scripts/enrich-address-from-coords.mjs cho các complex seed từ
-- V7.6.1 chỉ có address = tên thành phố trần trụi (OSM gốc không có tag addr:*
-- chi tiết cho các địa điểm này).
--
-- address: ghép từ road/house_number THẬT lấy qua Nominatim reverse-geocode.
-- district: ƯỚC LƯỢNG bằng nearest-centroid (toạ độ thật của complex so với toạ độ
-- trung tâm gần đúng của từng quận/huyện CŨ) — KHÔNG phải tra cứu ranh giới hành
-- chính chính xác. Lý do phải ước lượng thay vì lấy trực tiếp từ Nominatim: Việt Nam
-- đã sáp nhập/bãi bỏ cấp quận/huyện ngoài thực tế (chỉ còn tỉnh/thành -> xã/phường),
-- nên Nominatim hiện trả về cấu trúc MỚI không khớp với VietnamLocationReference.java
-- (dự án chủ động giữ cấu trúc quận/huyện CŨ cho mục đích học thuật/đơn giản hoá).
-- 0 complex bị lỗi request, giữ nguyên address cũ — xem
-- scripts/output/address-enrichment.json (status=ERROR) nếu cần xử lý lại.
-- Match theo (name, latitude, longitude) — 3 giá trị ổn định từ lúc seed, không cần biết complex_id.

UPDATE stadium_complexes SET address = 'Ngõ 122 Phố Vĩnh Tuy, Hai Bà Trưng, Hà Nội', district = 'Hai Bà Trưng' WHERE name = 'Sân Tennis Vĩnh Tuy' AND latitude = 20.9983768 AND longitude = 105.8747737 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 72 Trần Đại Nghĩa, Hai Bà Trưng, Hà Nội', district = 'Hai Bà Trưng' WHERE name = 'Sân Bóng rổ' AND latitude = 21.0004356 AND longitude = 105.8446627 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 72 Trần Đại Nghĩa, Hai Bà Trưng, Hà Nội', district = 'Hai Bà Trưng' WHERE name = 'Sân Bóng chuyền' AND latitude = 21.0005019 AND longitude = 105.8439939 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Lê Thanh Nghị, Hai Bà Trưng, Hà Nội', district = 'Hai Bà Trưng' WHERE name = 'Cầu lông & Bóng bàn' AND latitude = 21.0007837 AND longitude = 105.8440279 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Lương Thế Vinh, Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân vận động Đại học Hà Nội' AND latitude = 20.9895425 AND longitude = 105.795711 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Tân Mai, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng Tân Mai' AND latitude = 20.979882 AND longitude = 105.8507944 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Lương Thế Vinh, Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân Quần Vợt' AND latitude = 20.9890932 AND longitude = 105.7985485 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 12 Phố Nghĩa Dũng, Hoàn Kiếm, Hà Nội', district = 'Hoàn Kiếm' WHERE name = 'sân bóng đá Hồng Hà' AND latitude = 21.0474567 AND longitude = 105.8442976 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Đình Thôn, Nam Từ Liêm, Hà Nội', district = 'Nam Từ Liêm' WHERE name = 'lina' AND latitude = 21.0177339 AND longitude = 105.7751826 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 43 Đường Cổ Nhuế, Bắc Từ Liêm, Hà Nội', district = 'Bắc Từ Liêm' WHERE name = 'Sân bóng Phường Cổ Nhuế 2' AND latitude = 21.0584097 AND longitude = 105.7793913 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân vận động' AND latitude = 20.9612909 AND longitude = 105.8277583 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Phạm Văn Đồng, Cầu Giấy, Hà Nội', district = 'Cầu Giấy' WHERE name = 'Sân bóng Đại học Quốc gia Hà Nội' AND latitude = 21.0399434 AND longitude = 105.7821918 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân bóng Đầm Hồng 1' AND latitude = 20.9949689 AND longitude = 105.8232414 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Trần Đại Nghĩa, Hai Bà Trưng, Hà Nội', district = 'Hai Bà Trưng' WHERE name = 'Sân bóng rổ KTX Bách Khoa' AND latitude = 21.0058324 AND longitude = 105.8461918 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Văn Yên, Hà Đông, Hà Nội', district = 'Hà Đông' WHERE name = 'Sân bóng Văn Quán' AND latitude = 20.9737764 AND longitude = 105.7864465 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân bóng Mật Mã' AND latitude = 20.9812571 AND longitude = 105.7970208 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Yên Xá, Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân bóng BACVIET' AND latitude = 20.9731564 AND longitude = 105.7933136 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Lê Hồng Phong, Hà Đông, Hà Nội', district = 'Hà Đông' WHERE name = 'Sân bóng Cầu Đơ' AND latitude = 20.9685325 AND longitude = 105.7766846 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Bạch Thái Bưởi, Hà Đông, Hà Nội', district = 'Hà Đông' WHERE name = 'Sân bóng Phúc La' AND latitude = 20.9695306 AND longitude = 105.7872813 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân bóng Mộ Lao' AND latitude = 20.9811476 AND longitude = 105.7825995 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Khu Đô Thị Văn Quán, Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân bóng Zone9' AND latitude = 20.9721716 AND longitude = 105.7934397 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Nguyễn Văn Trỗi, Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân bóng Bưu Chính' AND latitude = 20.9806281 AND longitude = 105.7867231 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Ngô Thì Sỹ, Hà Đông, Hà Nội', district = 'Hà Đông' WHERE name = 'Sân bóng Vạn Phúc' AND latitude = 20.9790639 AND longitude = 105.7789343 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân Giáp Nhất' AND latitude = 21.0047359 AND longitude = 105.8138885 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Nguyễn Ngọc Doãn, Đống Đa, Hà Nội', district = 'Đống Đa' WHERE name = 'Sân bóng HVNH' AND latitude = 21.0097028 AND longitude = 105.8299115 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Phạm Văn Đồng, Cầu Giấy, Hà Nội', district = 'Cầu Giấy' WHERE name = 'Sân bóng Đại học Quốc gia Hà Nội' AND latitude = 21.0399352 AND longitude = 105.7824396 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Đặng Tiến Đông, Đống Đa, Hà Nội', district = 'Đống Đa' WHERE name = 'Sân bóng Thái Hà' AND latitude = 21.0154712 AND longitude = 105.8193424 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 144 Phố Tây Trà, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng The Garden' AND latitude = 20.9749798 AND longitude = 105.8805128 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 232 Phố Yên Hòa, Cầu Giấy, Hà Nội', district = 'Cầu Giấy' WHERE name = 'Sân bóng Yên Hòa' AND latitude = 21.0233223 AND longitude = 105.7914136 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Nguyễn Hiền, Hai Bà Trưng, Hà Nội', district = 'Hai Bà Trưng' WHERE name = 'Sân vận động Bách Khoa' AND latitude = 21.0022011 AND longitude = 105.8478568 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Phạm Văn Đồng, Bắc Từ Liêm, Hà Nội', district = 'Bắc Từ Liêm' WHERE name = 'sân bóng 8' AND latitude = 21.074979 AND longitude = 105.7862006 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngách 401/72 Đường Xuân Đỉnh, Bắc Từ Liêm, Hà Nội', district = 'Bắc Từ Liêm' WHERE name = 'san bong 12' AND latitude = 21.0727689 AND longitude = 105.7865671 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Nguyễn Văn Trỗi, Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân B5' AND latitude = 20.9808305 AND longitude = 105.7865746 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 161 Phố Trần Hòa, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng Đại Từ Đặng Xuân Bảng' AND latitude = 20.9741163 AND longitude = 105.8275995 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 99 Phố Định Công Hạ, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng đá 99 Định Công' AND latitude = 20.9813678 AND longitude = 105.8276689 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngách 31/230 Phố Định Công Thượng, Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân bóng Thông tin' AND latitude = 20.9831661 AND longitude = 105.8251839 AND district IS NULL;
UPDATE stadium_complexes SET address = 'KĐT Đền Lừ II, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng chuyền hồ Đền Lừ' AND latitude = 20.9843249 AND longitude = 105.8570206 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường 2.1 Gamuda Garden, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng Huy Hoàng - Gamuda' AND latitude = 20.9745652 AND longitude = 105.8810135 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Lạc Hồng, Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân bóng La Thành' AND latitude = 20.9916596 AND longitude = 105.82607 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Hạ Trại, Long Biên, Hà Nội', district = 'Long Biên' WHERE name = 'Thong Nhat Football Field' AND latitude = 21.0122788 AND longitude = 105.8991252 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Phú Viên, Hoàn Kiếm, Hà Nội', district = 'Hoàn Kiếm' WHERE name = 'Sân bóng đá Ven Đê 3' AND latitude = 21.0297471 AND longitude = 105.8713054 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Phú Viên, Hoàn Kiếm, Hà Nội', district = 'Hoàn Kiếm' WHERE name = 'Sân Bóng Ven Đê 2' AND latitude = 21.0306572 AND longitude = 105.8727501 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Trịnh Hoài Đức, Đống Đa, Hà Nội', district = 'Đống Đa' WHERE name = 'Trung tâm huấn luyện Vận động viên' AND latitude = 21.029618 AND longitude = 105.8318298 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Nam Từ Liêm, Hà Nội', district = 'Nam Từ Liêm' WHERE name = 'Sân bóng Văn phòng Quốc hội' AND latitude = 21.0326405 AND longitude = 105.7536986 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Lê Quang Đạo, Nam Từ Liêm, Hà Nội', district = 'Nam Từ Liêm' WHERE name = 'Mễ Trì Park의 종합 운동장' AND latitude = 21.0065406 AND longitude = 105.7727079 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Nguyễn Vĩnh Bảo, Cầu Giấy, Hà Nội', district = 'Cầu Giấy' WHERE name = 'Sân Bóng Rổ' AND latitude = 21.0170123 AND longitude = 105.8006103 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Lối ra xe máy Học viện Ngoại giao, Cầu Giấy, Hà Nội', district = 'Cầu Giấy' WHERE name = 'Sân bóng chuyền' AND latitude = 21.0224054 AND longitude = 105.8065652 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Đại Kim, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng Hồng Quang' AND latitude = 20.9736527 AND longitude = 105.8319145 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ Hạnh Phúc, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân Bóng Đền Lừ 3' AND latitude = 20.9829754 AND longitude = 105.8547282 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Tân Mai, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng đá CLB Thể thao Thiếu nhi Tân Mai' AND latitude = 20.9849096 AND longitude = 105.8486726 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Phạm Tu, Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân bóng Đại Nam' AND latitude = 20.968646 AND longitude = 105.802856 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hà Đông, Hà Nội', district = 'Hà Đông' WHERE name = 'Sân bóng Văn La' AND latitude = 20.9549041 AND longitude = 105.7637715 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 2 Phố Gia Quất, Long Biên, Hà Nội', district = 'Long Biên' WHERE name = 'Xoài Arena - Tổ hợp sân Cầu Lông' AND latitude = 21.051603 AND longitude = 105.8814885 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngách 20 Ngõ Trại Cá, Hai Bà Trưng, Hà Nội', district = 'Hai Bà Trưng' WHERE name = 'Sân Kho' AND latitude = 20.9938095 AND longitude = 105.8461755 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Khu công nghiệp Hoàng Mai, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân Bóng Đá HLP - Đồng Phát' AND latitude = 20.9809594 AND longitude = 105.869465 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 587 Đường Tam Trinh, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng AKKA Vĩnh Hoàng' AND latitude = 20.9807574 AND longitude = 105.8683869 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Tây Hồ, Hà Nội', district = 'Tây Hồ' WHERE name = 'Sân bóng aladdin' AND latitude = 21.075547 AND longitude = 105.8331027 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Hoàng Mai, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng đá Quân đội Hoàng Mai' AND latitude = 20.9875243 AND longitude = 105.8602804 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hoàn Kiếm, Hà Nội', district = 'Hoàn Kiếm' WHERE name = 'Sân bóng Tin Lớn' AND latitude = 21.028581 AND longitude = 105.8738438 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Vành đai 2.5, Thanh Xuân, Hà Nội', district = 'Thanh Xuân' WHERE name = 'Sân bóng Đầm Hồng 2' AND latitude = 20.9905526 AND longitude = 105.8250253 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 161 Phố Trần Hòa, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng Định Công 2' AND latitude = 20.9755773 AND longitude = 105.8272282 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 175 Phố Định Công, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng Hoa Mừng' AND latitude = 20.9800479 AND longitude = 105.838861 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân Tennis-Pickleball Đại Kim' AND latitude = 20.9776264 AND longitude = 105.836946 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 48 Phố Hưng Thịnh, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng BCH' AND latitude = 20.9603972 AND longitude = 105.8650004 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 196 Đường Lĩnh Nam, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng Bảo Lâm' AND latitude = 20.9824704 AND longitude = 105.8746642 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Ngõ 378 Đường Lĩnh Nam, Hoàng Mai, Hà Nội', district = 'Hoàng Mai' WHERE name = 'Sân bóng Thành Nam' AND latitude = 20.9801656 AND longitude = 105.8814925 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phố Dương Văn Bé, Hai Bà Trưng, Hà Nội', district = 'Hai Bà Trưng' WHERE name = 'Sân cầu lông công viên Vĩnh Hoàng' AND latitude = 20.9941367 AND longitude = 105.875442 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Kiệt 35 Trưng Nhị, Hải Châu, Đà Nẵng', district = 'Hải Châu' WHERE name = 'Tiểu La' AND latitude = 16.04498 AND longitude = 108.2129961 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Đinh Núp, Thanh Khê, Đà Nẵng', district = 'Thanh Khê' WHERE name = 'Sân vận động Thanh Khê' AND latitude = 16.0609768 AND longitude = 108.1831643 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Nam Kỳ Khởi Nghĩa, Ngũ Hành Sơn, Đà Nẵng', district = 'Ngũ Hành Sơn' WHERE name = 'San Tennis' AND latitude = 15.976461 AND longitude = 108.2515296 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Đinh Núp, Thanh Khê, Đà Nẵng', district = 'Thanh Khê' WHERE name = 'Sân Tennis khu Vui chơi Phần Lăng' AND latitude = 16.062626 AND longitude = 108.183059 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hải Châu, Đà Nẵng', district = 'Hải Châu' WHERE name = 'Sân bóng rổ Quân Khu 5' AND latitude = 16.046809 AND longitude = 108.2159443 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Cẩm Lệ, Đà Nẵng', district = 'Cẩm Lệ' WHERE name = 'Sân vận động Chi Lăng' AND latitude = 15.9992598 AND longitude = 108.2233504 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đào Trí, Nhà Bè, Hồ Chí Minh', district = 'Nhà Bè' WHERE name = 'Sân Bóng Mini Nghĩa Thảo' AND latitude = 10.7110943 AND longitude = 106.742877 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Quận 4, Hồ Chí Minh', district = 'Quận 4' WHERE name = 'Câu Lạc Bộ Cầu Lông T793' AND latitude = 10.7471263 AND longitude = 106.6985988 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Nguyễn Thiện Thành, Quận 4, Hồ Chí Minh', district = 'Quận 4' WHERE name = 'Sân Bóng đá & Bóng rổ Empire City' AND latitude = 10.7681019 AND longitude = 106.7120952 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Gò Vấp, Hồ Chí Minh', district = 'Gò Vấp' WHERE name = 'Sân Bóng Thanh Niên 2' AND latitude = 10.8539822 AND longitude = 106.6736209 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hẻm 39/4 Đường Đặng Thùy Trâm, Bình Thạnh, Hồ Chí Minh', district = 'Bình Thạnh' WHERE name = 'Outdoor badminton court' AND latitude = 10.8270363 AND longitude = 106.7018339 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Quận 10, Hồ Chí Minh', district = 'Quận 10' WHERE name = 'Sân bóng' AND latitude = 10.7726081 AND longitude = 106.6593159 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hẻm 186 Mễ Cốc, Quận 8, Hồ Chí Minh', district = 'Quận 8' WHERE name = 'Rạch Cát Badminton Club' AND latitude = 10.7148856 AND longitude = 106.626184 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Dương Văn An, Quận 2, Hồ Chí Minh', district = 'Quận 2' WHERE name = 'Sân Tennis' AND latitude = 10.7982847 AND longitude = 106.7407192 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường số 2, Nhà Bè, Hồ Chí Minh', district = 'Nhà Bè' WHERE name = 'Sân bóng đá Phú Mỹ Q7 (sân cát)' AND latitude = 10.7122932 AND longitude = 106.7403758 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phạm Khắc, Quận 1, Hồ Chí Minh', district = 'Quận 1' WHERE name = 'Sân vận động Hoa Lư' AND latitude = 10.7879111 AND longitude = 106.7016179 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Huyền Trân Công Chúa, Quận 1, Hồ Chí Minh', district = 'Quận 1' WHERE name = 'Sân vận động Tao Đàn' AND latitude = 10.7752098 AND longitude = 106.6947592 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Tân Bình, Hồ Chí Minh', district = 'Tân Bình' WHERE name = 'CLB Quần Vợt Viettel' AND latitude = 10.8078004 AND longitude = 106.6491171 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hoàng Hoa Thám, Tân Bình, Hồ Chí Minh', district = 'Tân Bình' WHERE name = 'Sân bóng Hoàng Hoa Thám' AND latitude = 10.805219 AND longitude = 106.6485883 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Phan Thúc Duyện, Tân Bình, Hồ Chí Minh', district = 'Tân Bình' WHERE name = 'Sân Bóng Đá Chảo Lửa' AND latitude = 10.8059337 AND longitude = 106.6584014 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Quận 12, Hồ Chí Minh', district = 'Quận 12' WHERE name = 'Sân banh Cây Đa' AND latitude = 10.8630238 AND longitude = 106.618905 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường D6, Quận 7, Hồ Chí Minh', district = 'Quận 7' WHERE name = 'Sân vận động' AND latitude = 10.7326155 AND longitude = 106.6977518 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Nguyễn Viết Xuân, Thủ Đức, Hồ Chí Minh', district = 'Thủ Đức' WHERE name = 'Sân banh Phường Bình Đường' AND latitude = 10.8676011 AND longitude = 106.7488298 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hẻm 97/57 Đường Số 8, Thủ Đức, Hồ Chí Minh', district = 'Thủ Đức' WHERE name = 'Sân bóng đá Tiến Phát' AND latitude = 10.8349791 AND longitude = 106.7733979 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hẻm 1096, Thủ Đức, Hồ Chí Minh', district = 'Thủ Đức' WHERE name = 'Sân Vận Động' AND latitude = 10.856552 AND longitude = 106.7589935 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường D2, Bình Thạnh, Hồ Chí Minh', district = 'Bình Thạnh' WHERE name = 'Sân cỏ nhân tạo Thiên Trường' AND latitude = 10.8093921 AND longitude = 106.7172565 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Tân Sơn, Tân Bình, Hồ Chí Minh', district = 'Tân Bình' WHERE name = 'Sân 320' AND latitude = 10.81882 AND longitude = 106.63904 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường số 10, Quận 6, Hồ Chí Minh', district = 'Quận 6' WHERE name = 'Sân quần vợt Thanh Lam' AND latitude = 10.7493457 AND longitude = 106.6176579 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hẻm 16 Trần Thiện Chánh, Quận 10, Hồ Chí Minh', district = 'Quận 10' WHERE name = 'Lan Anh' AND latitude = 10.7709562 AND longitude = 106.6712581 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Quận 10, Hồ Chí Minh', district = 'Quận 10' WHERE name = 'Lan Anh' AND latitude = 10.7710333 AND longitude = 106.6714143 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Thủ Đức, Hồ Chí Minh', district = 'Thủ Đức' WHERE name = 'sân bóng đá' AND latitude = 10.884076 AND longitude = 106.7760982 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Số 13, Bình Tân, Hồ Chí Minh', district = 'Bình Tân' WHERE name = 'Sân Bóng Đá Mini Nguyễn Hữu Đan' AND latitude = 10.7446349 AND longitude = 106.6107209 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Dương Thị Mười, Quận 12, Hồ Chí Minh', district = 'Quận 12' WHERE name = 'Nhà thi đấu quận 12' AND latitude = 10.8738062 AND longitude = 106.6384929 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường D6, Quận 7, Hồ Chí Minh', district = 'Quận 7' WHERE name = 'Sân tennis' AND latitude = 10.7323834 AND longitude = 106.6971511 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Bình Thạnh, Hồ Chí Minh', district = 'Bình Thạnh' WHERE name = 'Sân bóng đá Phan Chu Trinh' AND latitude = 10.8151134 AND longitude = 106.7013745 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Quận 9, Hồ Chí Minh', district = 'Quận 9' WHERE name = 'Mega Ruby Khang Điền Tennis Court' AND latitude = 10.8003395 AND longitude = 106.7943707 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường số 16, Thủ Đức, Hồ Chí Minh', district = 'Thủ Đức' WHERE name = 'sân bóng' AND latitude = 10.8884705 AND longitude = 106.7640464 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường D6, Quận 7, Hồ Chí Minh', district = 'Quận 7' WHERE name = 'Sân bóng rổ' AND latitude = 10.732373 AND longitude = 106.696968 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Nguyễn Lương Bằng, Quận 7, Hồ Chí Minh', district = 'Quận 7' WHERE name = 'Sân bóng rổ' AND latitude = 10.723607 AND longitude = 106.724392 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Cầu Cả Cấm 2, Quận 7, Hồ Chí Minh', district = 'Quận 7' WHERE name = 'Sân tennis' AND latitude = 10.7230933 AND longitude = 106.7241133 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Tân Phú, Hồ Chí Minh', district = 'Tân Phú' WHERE name = 'Sân bóng đá Sport Plus WeSport' AND latitude = 10.8049534 AND longitude = 106.6122377 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường số 21, Bình Tân, Hồ Chí Minh', district = 'Bình Tân' WHERE name = 'Sân bóng đá Mã Lò' AND latitude = 10.7844559 AND longitude = 106.5971502 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường M7, Tân Phú, Hồ Chí Minh', district = 'Tân Phú' WHERE name = 'Sân quần vợt TANIMEX' AND latitude = 10.8168558 AND longitude = 106.6087079 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường 18E, Tân Bình, Hồ Chí Minh', district = 'Tân Bình' WHERE name = 'Sân bóng Chảo Lửa' AND latitude = 10.8066361 AND longitude = 106.6556877 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường Số 9, Bình Thạnh, Hồ Chí Minh', district = 'Bình Thạnh' WHERE name = 'Sân bóng chuyền khu phố 3' AND latitude = 10.8217351 AND longitude = 106.7193542 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường số 12, Quận 8, Hồ Chí Minh', district = 'Quận 8' WHERE name = 'Sân bóng đá 13C' AND latitude = 10.715664 AND longitude = 106.6427684 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Quận 2, Hồ Chí Minh', district = 'Quận 2' WHERE name = 'Sân Tennis Khu Nam Long Q9' AND latitude = 10.8049786 AND longitude = 106.7795937 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường số 2, Quận 12, Hồ Chí Minh', district = 'Quận 12' WHERE name = 'Sân bóng Cao đẳng Saigon Tech' AND latitude = 10.8558048 AND longitude = 106.6305917 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hẻm 502/23 Đường Huỳnh Tấn Phát, Quận 7, Hồ Chí Minh', district = 'Quận 7' WHERE name = 'Sân bóng đá NTĐ Quận 7' AND latitude = 10.7409141 AND longitude = 106.7290608 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hẻm 502/11 Đường Huỳnh Tấn Phát, Quận 7, Hồ Chí Minh', district = 'Quận 7' WHERE name = 'Sân bóng chuyền ngoài trời NTĐ Quận 7' AND latitude = 10.7412496 AND longitude = 106.7289678 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hẻm 502 Huỳnh Tấn Phát, Quận 7, Hồ Chí Minh', district = 'Quận 7' WHERE name = 'Sân bóng rổ ngoài trời NTĐ Quận 7' AND latitude = 10.7418986 AND longitude = 106.7293404 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hoàng Trọng Mậu, Quận 7, Hồ Chí Minh', district = 'Quận 7' WHERE name = 'RMIT University Football Pitch' AND latitude = 10.7297395 AND longitude = 106.6916471 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hoàng Trọng Mậu, Quận 4, Hồ Chí Minh', district = 'Quận 4' WHERE name = 'Sân Banh Đại Học Cảnh Sát' AND latitude = 10.7350765 AND longitude = 106.6958215 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường số 9, Quận 2, Hồ Chí Minh', district = 'Quận 2' WHERE name = 'Sân cầu lông' AND latitude = 10.7930768 AND longitude = 106.7739263 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường 12A, Quận 2, Hồ Chí Minh', district = 'Quận 2' WHERE name = 'Sân bóng rổ' AND latitude = 10.7925802 AND longitude = 106.7714126 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Trần Đại Nghĩa, Thủ Đức, Hồ Chí Minh', district = 'Thủ Đức' WHERE name = 'Sân thể thao UEL' AND latitude = 10.8726028 AND longitude = 106.777544 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Lương Văn Can, Thủ Đức, Hồ Chí Minh', district = 'Thủ Đức' WHERE name = 'Nhà thể thao UEL' AND latitude = 10.8727344 AND longitude = 106.7779004 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Lương Văn Can, Thủ Đức, Hồ Chí Minh', district = 'Thủ Đức' WHERE name = 'Sân bóng rổ UEL' AND latitude = 10.8727969 AND longitude = 106.777692 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Thủ Đức, Hồ Chí Minh', district = 'Thủ Đức' WHERE name = 'Sân bóng đá UEL' AND latitude = 10.8724568 AND longitude = 106.7775869 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Tân Phú, Hồ Chí Minh', district = 'Tân Phú' WHERE name = 'Sân cầu lông Tây Thạnh' AND latitude = 10.8101273 AND longitude = 106.6265994 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hẻm 272 Gò Xoài, Tân Phú, Hồ Chí Minh', district = 'Tân Phú' WHERE name = 'Sân bóng đá hẻm 278 Gò Xoài' AND latitude = 10.7896183 AND longitude = 106.6055786 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đường số 21, Bình Tân, Hồ Chí Minh', district = 'Bình Tân' WHERE name = 'Sân bóng đá Mã Lò' AND latitude = 10.7843274 AND longitude = 106.596581 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hẻm 114 Phạm Văn Chiêu, Gò Vấp, Hồ Chí Minh', district = 'Gò Vấp' WHERE name = 'Sân Tennis Quyết Tâm' AND latitude = 10.8461487 AND longitude = 106.6462674 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Quận 9, Hồ Chí Minh', district = 'Quận 9' WHERE name = 'Sân bóng Kiểm sát' AND latitude = 10.8501 AND longitude = 106.7930896 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đào Trí, Nhà Bè, Hồ Chí Minh', district = 'Nhà Bè' WHERE name = 'Mansion Sports Club' AND latitude = 10.7161123 AND longitude = 106.7446185 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Hẻm 9 Đường Số 66, Bình Thạnh, Hồ Chí Minh', district = 'Bình Thạnh' WHERE name = 'Sân Tennis 66' AND latitude = 10.8141432 AND longitude = 106.7277289 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Quận 9, Hồ Chí Minh', district = 'Quận 9' WHERE name = 'San bóng ngoài trời' AND latitude = 10.8695042 AND longitude = 106.7911486 AND district IS NULL;
UPDATE stadium_complexes SET address = 'Đông Tây 1, Quận 2, Hồ Chí Minh', district = 'Quận 2' WHERE name = 'Tennis Fans League' AND latitude = 10.7987537 AND longitude = 106.7481629 AND district IS NULL;
