# 🎯 AI Chatbot Evaluation Harness

Bộ khung kiểm thử hành vi thực tế của LLM (Groq) giúp tự động hóa việc chấm điểm và bảo vệ hệ thống chatbot chống lỗi hồi quy (regression).

---

## 📁 Cấu trúc thư mục

*   `scenarios.json`: Chứa các kịch bản kiểm thử (scenarios). Mỗi kịch bản có một hoặc nhiều lượt chat (`turns`) và các điều kiện mong đợi (`expect`).
*   `run_eval.py`: Thực hiện cuộc gọi API thực tế tới Backend, gửi kèm lịch sử (`history`) và phiên (`X-Session-ID`), tự động kiểm tra `expect` và xuất kết quả ra `snapshots/latest.json`.
*   `report_eval.py`: So sánh kết quả chạy mới nhất (`latest.json`) với kết quả chuẩn (`snapshots/baseline.json`).
*   `snapshots/`: Thư mục lưu kết quả chạy.

---

## ⚙️ Hướng dẫn sử dụng

### 1. Khởi động môi trường
Harness kiểm thử trực tiếp qua API thật, vì vậy các dịch vụ bắt buộc phải chạy:
```bash
# 1. Chạy Postgres, Redis
docker compose -f docker-compose.infra.yml up -d

# 2. Khởi động Spring Boot Backend (nghe tại cổng 8080)
cd backend
./mvnw spring-boot:run
```

### 2. Thực hiện chạy kiểm thử (Chạy đúng 1 lần khi cần)
Do cuộc gọi tới Groq thật bị giới hạn rate limit, `run_eval.py` được thiết kế giãn cách **25 giây** mỗi request để bảo vệ quota. Quá trình chạy mất khoảng **6 - 8 phút**:
```bash
cd ai-eval
python run_eval.py
```

### 3. Xem báo cáo & So sánh với Baseline
```bash
python report_eval.py
```
Nếu có sự giảm sút chất lượng (ví dụ một câu hỏi trước đây PASS nhưng nay lại FAIL), script sẽ in chi tiết và kết thúc với mã lỗi `1`.

### 4. Thiết lập Baseline lần đầu (hoặc cập nhật khi thay đổi prompt chủ động)
Nếu bạn thay đổi prompt và chấp nhận kết quả mới nhất là đúng chuẩn, chạy lệnh sau để biến kết quả vừa chạy thành baseline chuẩn mới:
```bash
python report_eval.py --promote
```

---

## 🛠️ Danh sách các bug được chống hồi quy (Anti-regression Bugs)

Harness bao gồm 5 kịch bản kiểm thử tương ứng để phòng chống 5 bug đã phát hiện qua QA:
1.  **`bug-price-hallucination`**: Cấm LLM tự bịa ra giá sân hoặc địa điểm mặc định (Hà Nội/HCM) khi người dùng chỉ hỏi thông tin chung ở Đà Nẵng.
2.  **`bug-deictic-reference`**: Phân giải chính xác đại từ chỉ định *"sân này"* thành ID thật của sân đầu tiên trong danh sách hiển thị trước đó thông qua Redis cache.
3.  **`bug-faq-escalation-mixup`**: Phân biệt chính sách hủy sân (FAQ) không được nhầm sang chính sách khiếu nại (Complaints).
4.  **`bug-skip-need-more-info`**: Chặn LLM đoán bừa khi thông tin cơ bản (môn chơi/khu vực) bị thiếu, ép trả về intent `need_more_info` để hỏi lại.
5.  **`bug-abuse-escalation`**: Cấm LLM tự giới thiệu Hotline/CSKH khi câu hỏi hoàn toàn có sẵn trong FAQ chính sách hủy.
