### MỘT SỐ VÍ DỤ QUAN TRỌNG ĐỂ PHÂN LOẠI INTENT (FEW-SHOT EXAMPLES)

User: "Tìm sân bóng Thủ Đức"
AI: {"intent": "search_stadiums", "confidence": 0.98, "message": "Để mình tìm các sân bóng đá ở Thủ Đức cho bạn nhé.", "params": {"sportName": "Bóng đá", "district": "Thủ Đức"}}

User: "đặt giúp tôi sân bóng đá thủ đức, 14 giờ ngày mai" (Hoặc bất kỳ câu nào có động từ "đặt", "book", "giữ chỗ" + tên sân + giờ)
AI: {"intent": "create_booking", "confidence": 0.95, "message": "Bạn đợi một chút để mình lên nháp đặt sân nhé.", "params": {"targetIndex": 0, "startTime": "14:00", "date": "YYYY-MM-DD"}} // Chú ý: TUYỆT ĐỐI KHÔNG dùng search_stadiums khi có động từ đặt sân, cho dù câu có chứa keyword tên sân.

User: "Cho xem giờ trống của sân đầu tiên vào chiều thứ 7"
AI: {"intent": "get_slots", "confidence": 0.99, "message": "Đây là các giờ còn trống của sân đầu tiên vào thứ 7 tuần này:", "params": {"targetIndex": 0, "date": "YYYY-MM-DD"}}

User: "có, là sân cẩm lệ" (Trả lời cho câu hỏi xác nhận tên sân từ AI để đặt sân)
AI: {"intent": "create_booking", "confidence": 0.92, "message": "Để mình lên nháp đặt sân Cẩm Lệ nhé.", "params": {"keyword": "Cẩm Lệ"}}

User: "đặt lại slot 14h chiều mai" (Trả lời sau khi AI báo lỗi hoặc yêu cầu chọn lại giờ)
AI: {"intent": "create_booking", "confidence": 0.90, "message": "Để mình đặt lại khung giờ 14h chiều mai nhé.", "params": {"startTime": "14:00", "date": "YYYY-MM-DD"}}

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

User: "giá thuê sân bóng rổ thủ đức là bao nhiêu"
AI: {"intent": "get_price", "confidence": 0.97, "message": "Để mình kiểm tra giá sân bóng rổ ở Thủ Đức cho bạn nhé.", "params": {"sportName": "Bóng rổ", "district": "Thủ Đức"}}

User: "thường thì mấy giờ sân cầu lông vắng người"
AI: {"intent": "recommend_time", "confidence": 0.95, "message": "Theo dữ liệu của mình, đây là các khung giờ sân cầu lông thường ít người chơi:", "params": {"sportName": "Cầu lông"}}
