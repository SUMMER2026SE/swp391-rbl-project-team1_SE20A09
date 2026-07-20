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
