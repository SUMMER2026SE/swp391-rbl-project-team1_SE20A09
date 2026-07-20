User: "Đặt sân bóng đá 5 người ở Đà Nẵng, 19h chủ nhật tuần này" (KHÔNG có tên sân cụ thể)
AI: {"intent": "search_stadiums", "confidence": 0.98, "message": "Để mình tìm các sân bóng đá 5 người ở Đà Nẵng vào chủ nhật tuần này nhé.", "params": {"sportName": "Bóng đá", "province": "Đà Nẵng", "footballFieldType": "FIVE_A_SIDE", "startTime": "19:00", "targetDate": "2026-07-26"}}

User: "Đặt cho tôi sân đầu tiên slot 19 giờ thứ 4 tuần này" (relative date + targetIndex + time)
AI: {"intent": "create_booking", "confidence": 0.98, "message": "Để mình lên nháp đặt sân đầu tiên vào thứ 4 tuần này lúc 19h nhé.", "params": {"targetIndex": 0, "startTime": "19:00", "date": "2026-07-22"}}

User: "đặt giúp tôi sân thứ 3 slot 15h tuần sau" (relative date với tuần sau)
AI: {"intent": "create_booking", "confidence": 0.95, "message": "Bạn đợi một chút để mình lên nháp đặt sân thứ 3 tuần sau nhé.", "params": {"startTime": "15:00", "date": "2026-07-28"}}

User: "đặt sân bóng đá ngày mai, 15h" (relative date + time)
AI: {"intent": "create_booking", "confidence": 0.95, "message": "Bạn đợi một chút để mình lên nháp đặt sân ngày mai nhé.", "params": {"sportName": "Bóng đá", "startTime": "15:00", "date": "2026-07-21"}}

User: "đặt giúp tôi sân Cẩm Lệ, 14 giờ ngày mai" (CÓ tên sân cụ thể: "Cẩm Lệ" là tên sân)
AI: {"intent": "create_booking", "confidence": 0.95, "message": "Bạn đợi một chút để mình lên nháp đặt sân nhé.", "params": {"keyword": "Cẩm Lệ", "startTime": "14:00", "date": "2026-07-21"}}

User: "có, là sân cẩm lệ" (Trả lời cho câu hỏi xác nhận tên sân từ AI để đặt sân)
AI: {"intent": "create_booking", "confidence": 0.92, "message": "Để mình lên nháp đặt sân Cẩm Lệ nhé.", "params": {"keyword": "Cẩm Lệ"}}

User: "đặt lại slot 14h chiều mai" (Trả lời sau khi AI báo lỗi hoặc yêu cầu chọn lại giờ)
AI: {"intent": "create_booking", "confidence": 0.90, "message": "Để mình đặt lại khung giờ 14h chiều mai nhé.", "params": {"startTime": "14:00", "date": "2026-07-21"}}

User: "Cho xem giờ trống của sân đầu tiên vào chiều thứ 7"
AI: {"intent": "get_slots", "confidence": 0.99, "message": "Đây là các giờ còn trống của sân đầu tiên vào thứ 7 tuần này:", "params": {"targetIndex": 0, "date": "2026-07-25"}} 

User: "Cho xem giờ trống sân thứ 2 tuần sau"
AI: {"intent": "get_slots", "confidence": 0.97, "message": "Đây là các giờ còn trống vào thứ 2 tuần sau:", "params": {"date": "2026-07-27"}}

User: "tuần này tôi đã đặt những sân nào"
AI: {"intent": "my_bookings", "confidence": 0.98, "message": "Đây là danh sách các sân bạn đã đặt trong tuần này:", "params": {}}
