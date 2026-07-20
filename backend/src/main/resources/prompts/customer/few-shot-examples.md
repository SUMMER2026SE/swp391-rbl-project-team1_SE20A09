### MỘT SỐ VÍ DỤ QUAN TRỌNG ĐỂ PHÂN LOẠI INTENT (FEW-SHOT EXAMPLES)

⚠️⚠️⚠️ QUY TẮC SẮT (IRONCLAD RULE) - ĐỌC KỸ:
**"Đặt" + [môn] + [khu vực] = search_stadiums** (CHƯA có sân cụ thể)
**"Đặt" + [tên sân cụ thể] = create_booking** (ĐÃ có sân cụ thể)
→ KHÔNG BAO GIỜ dùng create_booking khi user chỉ nói "tìm/đặt sân [môn] ở [địa điểm]"

User: "Đặt sân bóng đá 5 người ở Đà Nẵng, 19h chủ nhật tuần này" (KHÔNG có tên sân cụ thể)
AI: {"intent": "search_stadiums", "confidence": 0.98, "message": "Để mình tìm các sân bóng đá 5 người ở Đà Nẵng vào chủ nhật tuần này nhé.", "params": {"sportName": "Bóng đá", "province": "Đà Nẵng", "footballFieldType": "FIVE_A_SIDE", "startTime": "19:00", "targetDate": "2026-07-26"}}

User: "Tìm sân bóng Thủ Đức"
AI: {"intent": "search_stadiums", "confidence": 0.98, "message": "Để mình tìm các sân bóng đá ở Thủ Đức cho bạn nhé.", "params": {"sportName": "Bóng đá", "district": "Thủ Đức"}}

User: "tìm sân cầu lông gần vị trí của tôi hiện tại" (có cụm "gần vị trí của tôi")
AI: {"intent": "search_stadiums", "confidence": 0.97, "message": "Để mình tìm các sân cầu lông gần bạn nhất nhé.", "params": {"sportName": "Cầu lông", "district": "CURRENT_LOCATION"}}

User: "sân bóng đá nào gần đây" (có cụm "gần đây")
AI: {"intent": "search_stadiums", "confidence": 0.96, "message": "Để mình tìm các sân bóng đá gần bạn nhé.", "params": {"sportName": "Bóng đá", "district": "CURRENT_LOCATION"}}

User: "tìm sân bóng đá 5 người ở quận 1" (có "5 người")
AI: {"intent": "search_stadiums", "confidence": 0.97, "message": "Để mình tìm các sân bóng đá 5 người ở Quận 1 nhé.", "params": {"sportName": "Bóng đá", "district": "Quận 1", "footballFieldType": "FIVE_A_SIDE"}}

User: "sân futsal nào rẻ nhất" (có "futsal")
AI: {"intent": "search_stadiums", "confidence": 0.97, "message": "Để mình tìm các sân Futsal với giá tốt nhất nhé.", "params": {"sportName": "Bóng đá", "footballFieldType": "FUTSAL", "sortBy": "price"}}

User: "tìm sân 7 người gần trường đại học" (có "7 người")
AI: {"intent": "search_stadiums", "confidence": 0.97, "message": "Để mình tìm các sân bóng đá 7 người gần trường đại học nhé.", "params": {"sportName": "Bóng đá", "district": "CURRENT_LOCATION", "footballFieldType": "SEVEN_A_SIDE"}}

User: "tìm sân bóng đá gần chỗ tôi đang đứng" (có cụm "gần chỗ tôi")
AI: {"intent": "search_stadiums", "confidence": 0.96, "message": "Để mình tìm sân bóng đá gần bạn nhất nhé.", "params": {"sportName": "Bóng đá", "district": "CURRENT_LOCATION"}}

User: "đặt giúp tôi sân Cẩm Lệ, 14 giờ ngày mai" (CÓ tên sân cụ thể: "Cẩm Lệ" là tên sân)
AI: {"intent": "create_booking", "confidence": 0.95, "message": "Bạn đợi một chút để mình lên nháp đặt sân nhé.", "params": {"keyword": "Cẩm Lệ", "startTime": "14:00", "date": "2026-07-21"}}

User: "đặt giúp tôi sân bóng đá ở thủ đức, 14 giờ ngày mai" (KHÔNG có tên sân cụ thể, "thủ đức" chỉ là quận)
AI: {"intent": "search_stadiums", "confidence": 0.95, "message": "Bạn đợi một chút để mình tìm các sân bóng đá ở Thủ Đức nhé.", "params": {"sportName": "Bóng đá", "district": "Thủ Đức", "startTime": "14:00", "date": "2026-07-21"}}

User: "Cho xem giờ trống của sân đầu tiên vào chiều thứ 7"
AI: {"intent": "get_slots", "confidence": 0.99, "message": "Đây là các giờ còn trống của sân đầu tiên vào thứ 7 tuần này:", "params": {"targetIndex": 0, "date": "2026-07-25"}}

User: "tìm sân bóng đá 5 người ở Đà Nẵng, 19h thứ 7 tuần này" (relative date)
AI: {"intent": "search_stadiums", "confidence": 0.98, "message": "Để mình tìm các sân bóng đá 5 người ở Đà Nẵng vào tối thứ 7 tuần này nhé.", "params": {"sportName": "Bóng đá", "province": "Đà Nẵng", "footballFieldType": "FIVE_A_SIDE", "startTime": "19:00", "targetDate": "2026-07-25"}}

User: "cho xem sân ở đây, thứ 7 tuần sau" (relative date)
AI: {"intent": "search_stadiums", "confidence": 0.97, "message": "Để mình tìm các sân gần bạn vào thứ 7 tuần sau nhé.", "params": {"district": "CURRENT_LOCATION", "targetDate": "2026-08-01"}}

User: "sân tennis ở hà nội nào trống t7" (province + sport + relative date)
AI: {"intent": "search_stadiums", "confidence": 0.98, "message": "Để mình tìm các sân tennis ở Hà Nội vào thứ 7 nhé.", "params": {"sportName": "Tennis", "province": "Hà Nội", "targetDate": "2026-07-25"}}

User: "đặt sân bóng đá ngày mai, 15h" (relative date)
AI: {"intent": "create_booking", "confidence": 0.95, "message": "Bạn đợi một chút để mình lên nháp đặt sân ngày mai nhé.", "params": {"sportName": "Bóng đá", "startTime": "15:00", "date": "2026-07-21"}}

User: "có, là sân cẩm lệ" (Trả lời cho câu hỏi xác nhận tên sân từ AI để đặt sân)
AI: {"intent": "create_booking", "confidence": 0.92, "message": "Để mình lên nháp đặt sân Cẩm Lệ nhé.", "params": {"keyword": "Cẩm Lệ"}}

User: "đặt lại slot 14h chiều mai" (Trả lời sau khi AI báo lỗi hoặc yêu cầu chọn lại giờ)
AI: {"intent": "create_booking", "confidence": 0.90, "message": "Để mình đặt lại khung giờ 14h chiều mai nhé.", "params": {"startTime": "14:00", "date": "2026-07-21"}}

User: "có kèo nào ở tân bình ko"
AI: {"intent": "find_match", "confidence": 0.96, "message": "Để mình tìm các kèo ghép ở Tân Bình nhé.", "params": {"location": "Tân Bình"}}

User: "mình rảnh chiều nay có kèo nào khu vực gần trường không, giá sinh viên thôi" (KHÔNG có tên môn)
AI: {"intent": "need_more_info", "confidence": 0.92, "message": "Bạn muốn tìm kèo cho môn thể thao nào vậy? Hệ thống đang có: Bóng đá, Cầu lông, Bóng rổ, Tennis, Bóng chuyền. Cho mình biết để tìm kèo phù hợp nhé.", "params": {}}

User: "có kèo bóng đá nào gần đại học FPT không" (CÓ tên môn + địa điểm)
AI: {"intent": "find_match", "confidence": 0.96, "message": "Để mình tìm kèo bóng đá gần Đại học FPT cho bạn nhé.", "params": {"sportName": "Bóng đá", "location": "FPT"}}

User: "cho mình tham gia kèo 2 với" (Tham gia vào kèo thứ 2 đã liệt kê)
AI: {"intent": "join_match", "confidence": 0.99, "message": "Để mình gửi yêu cầu tham gia kèo thứ 2 cho bạn nhé.", "params": {"matchIndex": 1}}

User: "xin slot kèo đầu nha pro" (Cách nói ngắn ngọn tham gia kèo)
AI: {"intent": "join_match", "confidence": 0.97, "message": "Để mình gửi yêu cầu tham gia kèo đầu tiên nhé.", "params": {"matchIndex": 0, "message": "xin slot nha pro"}}

User: "tuần này tôi đã đặt những sân nào"
AI: {"intent": "my_bookings", "confidence": 0.98, "message": "Đây là danh sách các sân bạn đã đặt trong tuần này:", "params": {}}

User: "đơn đặt sân của tôi duyệt chưa"
AI: {"intent": "booking_status", "confidence": 0.96, "message": "Để mình kiểm tra trạng thái đơn đặt sân của bạn nhé:", "params": {}}

User: "hủy giúp tôi sân bóng tối nay"
AI: {"intent": "cancel_booking", "confidence": 0.98, "message": "Bạn muốn hủy đơn đặt sân nào dưới đây?", "params": {}}

User: "đơn mã #000648" (user chọn đơn bằng mã cụ thể)
AI: {"intent": "cancel_booking", "confidence": 0.99, "message": "Bạn muốn hủy đơn đặt sân #000648?", "params": {"bookingId": 648}}

User: "hủy đơn đầu tiên đi" (user chọn đơn theo vị trí)
AI: {"intent": "cancel_booking", "confidence": 0.99, "message": "Bạn muốn hủy đơn đầu tiên?", "params": {"targetIndex": 0}}

User: "đơn sân 2 cung thể thao tiên sơn" (user mô tả đơn bằng tên sân)
AI: {"intent": "cancel_booking", "confidence": 0.97, "message": "Bạn muốn hủy đơn sân 2 Cung Thể Thao Tiên Sơn?", "params": {"keyword": "cung thể thao tiên sơn"}}

User: "có" (xác nhận hủy sau khi AI hỏi)
AI: {"intent": "cancel_booking", "confidence": 0.99, "message": "Mình đang tiến hành hủy đơn cho bạn nhé.", "params": {}}

User: "hủy luôn" (xác nhận nhanh)
AI: {"intent": "cancel_booking", "confidence": 0.98, "message": "Mình đang tiến hành hủy đơn cho bạn nhé.", "params": {}}

User: "giá thuê sân bóng rổ thủ đức là bao nhiêu"
AI: {"intent": "get_price", "confidence": 0.97, "message": "Để mình kiểm tra giá sân bóng rổ ở Thủ Đức cho bạn nhé.", "params": {"sportName": "Bóng rổ", "district": "Thủ Đức"}}

User: "thường thì mấy giờ sân cầu lông vắng người"
AI: {"intent": "recommend_time", "confidence": 0.95, "message": "Theo dữ liệu của mình, đây là các khung giờ sân cầu lông thường ít người chơi:", "params": {"sportName": "Cầu lông"}}

User: "tìm sân bóng đá 5 người ở Đà Nẵng, 19h thứ 7 tuần này" (relative date)
AI: {"intent": "search_stadiums", "confidence": 0.98, "message": "Để mình tìm các sân bóng đá 5 người ở Đà Nẵng vào tối thứ 7 tuần này nhé.", "params": {"sportName": "Bóng đá", "province": "Đà Nẵng", "footballFieldType": "FIVE_A_SIDE", "startTime": "19:00", "targetDate": "2026-07-25"}}

User: "cho xem sân ở đây, thứ 7 tuần sau" (relative date)
AI: {"intent": "search_stadiums", "confidence": 0.97, "message": "Để mình tìm các sân gần bạn vào thứ 7 tuần sau nhé.", "params": {"district": "CURRENT_LOCATION", "targetDate": "2026-08-01"}}

User: "Đặt sân bóng đá 5 người ở Đà Nẵng, 19h chủ nhật tuần này" (location + sport + field type + time + relative date - tất cả trong 1 câu)
AI: {"intent": "search_stadiums", "confidence": 0.98, "message": "Để mình tìm các sân bóng đá 5 người ở Đà Nẵng vào chủ nhật tuần này nhé.", "params": {"sportName": "Bóng đá", "province": "Đà Nẵng", "footballFieldType": "FIVE_A_SIDE", "startTime": "19:00", "targetDate": "2026-07-26"}}

User: "tìm sân cầu lông ở quận 1, giá dưới 200k" (location + sport + price filter)
AI: {"intent": "search_stadiums", "confidence": 0.98, "message": "Để mình tìm các sân cầu lông ở Quận 1 với giá dưới 200k nhé.", "params": {"sportName": "Cầu lông", "district": "Quận 1", "maxPrice": 200000}}

User: "sân tennis ở hà nội nào trống t7" (province + sport + relative date)
AI: {"intent": "search_stadiums", "confidence": 0.98, "message": "Để mình tìm các sân tennis ở Hà Nội vào thứ 7 nhé.", "params": {"sportName": "Tennis", "province": "Hà Nội", "targetDate": "2026-07-25"}}

User: "đặt sân bóng đá ngày mai, 15h" (relative date)
AI: {"intent": "create_booking", "confidence": 0.95, "message": "Bạn đợi một chút để mình lên nháp đặt sân ngày mai nhé.", "params": {"sportName": "Bóng đá", "startTime": "15:00", "date": "2026-07-21"}}
