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

---

## 11. Định nghĩa Hoàn thành (Definition of Done)

### Template chung áp dụng cho mọi mục trong kế hoạch này

Theo đúng chuẩn DoD đã có sẵn trong `GEMINI.md` (mục "Định nghĩa Hoàn thành"), mỗi mục fix/tính năng trong tài liệu này chỉ được coi là **Done** khi:

1. **Có test đi kèm**
   - Unit test hoặc integration test cho logic backend thay đổi.
   - Bắt buộc ưu tiên test tự động cho các mục đụng tính tiền/trạng thái trung tâm như mục 1, 2, 3.
   - Với bug frontend thuần hiển thị như mục 4, 5, có thể chấp nhận verify thủ công qua UI nếu khu vực đó chưa có test harness phù hợp, nhưng phải ghi rõ cách verify trong PR.

2. **Verify đủ tất cả màn hình bị ảnh hưởng, không chỉ một màn hình đại diện**
   - Nếu một bug liệt kê nhiều vị trí code/màn hình, phải test đủ từng vị trí.
   - Ví dụ mục 4.1 bug địa chỉ có nhiều nơi đọc dữ liệu booking/stadium: chi tiết booking, lịch sử booking, booking phía Owner, widget trang chủ. Không được sửa một chỗ rồi coi như toàn bộ bug đã xong.

3. **Regression check các màn hình/luồng liền kề**
   - Đặc biệt với mục 1 refund/cancel vì đụng vào `Booking`, `Payment`, `TimeSlot`.
   - Không merge nếu chưa test lại tối thiểu các luồng: đặt sân, hủy sân, xem lịch sử booking, owner dashboard, revenue report và complaint nếu có liên kết trạng thái booking.

4. **Không còn debug code thừa và build/test pass 100%**
   - Không còn `console.log`, `System.out.println()`, comment nháp hoặc code chết.
   - Build, lint, type-check, backend tests và các test mới/cũ liên quan phải pass theo đúng workflow hiện tại.

5. **Đã qua review PR chuẩn của team trước khi merge**
   - Theo `GEMINI.md`, PR phải được review/approve bởi thành viên phụ trách chính (Lượng hoặc Huy) hoặc reviewer được team chỉ định.
   - Thành viên sở hữu branch không tự merge code của mình khi chưa có xác nhận review.

### Lưu ý DoD riêng cho các mục rủi ro cao

- **Mục 1 (Refund/Cancel):** ngoài test logic tính %, bắt buộc test tay ít nhất 3 kịch bản tiền:
  - Khách tự hủy sát giờ.
  - Owner hủy vì sự cố.
  - Đơn `DEPOSIT` bị hủy.
  - Kết quả phải đối chiếu số tiền hiển thị với số tiền đúng theo bảng/chính sách ở mục 1.3 và 1.4.

- **Mục 2 (phí dịch vụ):** PR description phải có ghi chú rõ ràng rằng đơn lịch sử **không bị tính lại**, trừ khi team đã chốt chủ trương migration dữ liệu cũ. Không để reviewer phải tự suy đoán hành vi này.

- **Mục 5 (`owner/bookings` pagination):** DoD nên bao gồm test với dữ liệu giả lập tối thiểu khoảng vài trăm booking cho một Owner để xác nhận endpoint/UI không còn tải toàn bộ lịch sử và hiệu năng thực sự cải thiện so với seed data nhỏ.

### Ghi chú cho người thực hiện

- Mục 1 (Refund/Cancel) và mục 2 (phí dịch vụ) nên làm chung một đợt hoặc ít nhất được thiết kế chung, vì cùng phụ thuộc vào dữ liệu tiền trong `Payment.amount` và cách tách "phí nền tảng", "phần Owner nhận", "số tiền thực đã thu".
- Mục 6 (chat) nên xây một cơ chế context-card dùng chung (`SYSTEM` message + JSON payload) cho cả 3 use case: sân, kèo đấu, slot booking. Tránh làm riêng lẻ 3 lần rồi khó bảo trì.
- Các bug ở mục 4 và 5 là fix độc lập, rủi ro thấp hơn, có thể làm song song/bất cứ lúc nào mà không cần chờ quyết định thiết kế lớn.
