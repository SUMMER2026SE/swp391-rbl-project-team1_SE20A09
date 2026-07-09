## 10. Ước lượng độ lớn & đề xuất phân công

> **Giới hạn quan trọng:** đây là ước lượng độ lớn tương đối (**S/M/L**) dựa trên phạm vi thay đổi code, mức rủi ro và số quyết định thiết kế cần chốt trước. Đây **không** phải ước lượng thời gian theo ngày/sprint. Team cần tự quy đổi S/M/L sang sprint dựa trên velocity và năng lực thực tế.

| Mục | Độ lớn | Vì sao | Cần quyết định thiết kế trước khi code? |
|---|---|---|---|
| 1. Refund/Cancel | **L** | Đụng entity mới hoặc field mới cho nguyên nhân hủy, sửa nhiều service, thêm governance cho `OWNER_FAULT`, sửa công thức tiền/refund, thêm trạng thái thanh toán và cập nhật nhiều màn hình frontend. | Có — cần chốt governance `OWNER_FAULT`, chính sách refund và có tích hợp refund API thật ngay hay chưa. |
| 2. Phí dịch vụ & doanh thu | **M → L** | **M** nếu chỉ đổi công thức/hằng số; **L** nếu thêm cấu hình DB, tách platform fee khỏi owner take và cập nhật báo cáo/liên quan payment. | Có — đây là quyết định business bắt buộc trước khi code. |
| 3. Khiếu nại | **M → L** | **M** cho phần dễ như bỏ giới hạn `COMPLETED`, thêm phân trang, fix điều hướng; **L** nếu làm workflow xử lý đầy đủ theo phương án mới. | Có — cần chọn phương án workflow A/B/C trước. |
| 4. Bug hiển thị booking | **S** | Các bug là fix hiển thị/normalize dữ liệu, đã có pattern fallback sẵn, không cần thiết kế mới. | Không |
| 5. Phân trang | **S** | Có pattern mẫu ở `BookingHistoryList` và `useBookingHistory`; chủ yếu áp dụng lại state `page`, `size`, `totalPages`. | Không |
| 6. Chat/nhắn tin | **M** | Nhiều điểm vào chat khác nhau nhưng có thể giảm độ phức tạp nếu dùng chung cơ chế context-card cho stadium/match/owner-slot. | Có — cần chốt context-card xuất hiện một lần hay mỗi lần đổi ngữ cảnh. |
| 6.4 Chat badge Owner/Admin | **S** | Chỉ import và render lại `ChatBadge` đã có sẵn ở layout Owner/Admin. | Không |
| 7. Gap riêng Owner | **Đã gộp** | Các phần riêng Owner đã được tính vào mục 1, 2, 5 và 6. | Theo từng mục liên quan |
| 8. Map UX | **S → M** | Marker overlap là **S**; radius circle/fit bounds là **S/M** tùy mức hoàn thiện UX. Leaflet đã hỗ trợ sẵn. | Không |
| 9. Dọn code chết | **S** | Chỉ xóa route mock sau khi confirm không còn link/import/reference. | Không |

### Đề xuất phân công theo scope (suy luận, chưa xác nhận)

> Bảng dưới đây chỉ suy luận từ ví dụ quy ước nhánh trong `GEMINI.md` và các branch liên quan đang tồn tại. Đây **không phải** bảng phân công sprint đã được xác nhận. Trước khi đưa vào sprint, team cần chốt người phụ trách thật để tránh hiểu nhầm.

| Mục trong kế hoạch | Domain/branch khớp với scope | Gợi ý người phụ trách cần xác nhận |
|---|---|---|
| 1. Refund/Cancel | `feature/booking/*`, `feature/luong/report/process-refund`, `feature/integrate-vnpay-momo-refund` | Người phụ trách booking/refund phối hợp |
| 2. Phí dịch vụ & doanh thu | `feature/report/revenue`, `feature/report/owner-dashboard` | Người phụ trách report/revenue |
| 3. Khiếu nại | `feature/complaint/submit-complaint`, `feature/complaint/admin-process` | Người đang giữ các branch complaint |
| 4. Bug địa chỉ/booking display | `feature/stadium/*` + `feature/booking/*` | Stadium và booking phối hợp |
| 5. Phân trang | Booking, complaint, admin/owner dashboard | Chia theo màn hình/module |
| 6. Chat | Module chat hiện tại + các màn hình gọi chat | Người phụ trách chat phối hợp với owner/booking/community |
| 8. Map UX | `feature/venue/search-filter`, `feature/search/*`, `feature/filter/venue` | Người phụ trách search/venue |
| 9. Dọn code chết | Admin UI | Bất kỳ thành viên nào có thể nhận sau khi verify reference |

