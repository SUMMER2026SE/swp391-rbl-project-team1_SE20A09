# Pull Request: Refactor Mô hình phí dịch vụ & doanh thu (Mục 2)

## 📋 Mô Tả Thay Đổi (Overview)
PR này giải quyết triệt để vấn đề phí dịch vụ cố định 20k và sự thiếu minh bạch trong tính toán doanh thu thực tế của chủ sân (Owner Net Revenue) được nêu tại **Mục 2** trong tài liệu `docs/qa_findings_refactor_plan.md`.

Các cải tiến cốt lõi bao gồm:
1. **Phí dịch vụ động theo tỷ lệ %**: Thay thế phí nền tảng cố định 20.000đ thành **5% giá trị sân cơ sở**, áp dụng sàn tối thiểu là **10.000đ** và trần tối đa là **30.000đ**.
2. **Khấu trừ phí dịch vụ phi hoàn lại (Non-refundable Platform Fee)**: Khi khách tự hủy đơn (`CUSTOMER_REQUEST`), phí dịch vụ sẽ không được hoàn lại. Số tiền hoàn trả được tính trên phần tiền sân ròng (`totalPaid - serviceFee`) nhân với tỷ lệ phân tầng thời gian theo quy định. Đối với sự cố lỗi do chủ sân (`OWNER_FAULT`), khách hàng vẫn nhận lại 100% đầy đủ số tiền (bao gồm cả phí dịch vụ).
3. **Đồng bộ hóa ghi sổ và cổng thanh toán**: Chỉnh sửa đồng thời số tiền hoàn thực tế ở cả luồng ghi sổ DB (`processLocalCancellationTx`) và gọi cổng API VNPay/MoMo thật (`processGatewayRefundTx`).
4. **Loại bỏ hardcode & tách biệt doanh thu**: Lưu trực tiếp `service_fee` tại thời điểm tạo đơn vào bảng `bookings`. Cập nhật các câu lệnh tính doanh thu ròng của chủ sân trên dashboard loại trừ `b.serviceFee` của từng đơn thay vì trừ tham số cố định.

---

## 🛠️ Chi Tiết Các Thay Đổi (Detailed Changes)

### 1. Database (Flyway Migration)
* **[NEW] `V104__add_service_fee_to_bookings.sql`**: 
  * Thêm cột `service_fee` (`NUMERIC(10,2)`) vào bảng `bookings` làm trường lưu vết lịch sử.
  * Chạy update hồi tố tất cả booking cũ có `service_fee = 20000.00`.

### 2. Backend (Spring Boot)
* **`Booking.java`**: Khai báo thuộc tính `serviceFee` tương ứng với cột mới trong DB.
* **`BookingDetailResponse.java`**: Thêm trường `serviceFee` để trả về cho Frontend hiển thị chi tiết hóa đơn.
* **`BookingServiceImpl.java`**:
  * Triển khai hàm `calculateServiceFee(BigDecimal basePrice)` tính 5% (sàn 10k, trần 30k).
  * Trong `createBooking`: Tính phí dịch vụ động, cộng vào tổng tiền và lưu vào thực thể.
  * Trong `cancelBooking`: Trừ phí dịch vụ khỏi phần tiền hoàn ròng, đồng bộ giá trị hoàn sang DB âm và API cổng thanh toán.
* **`RefundServiceImpl.java`**:
  * Cập nhật `calculateRefund` để áp dụng công thức hoàn tiền phân tầng trên lượng tiền ròng đã khấu trừ phí dịch vụ đối với lỗi của khách.
  * Cập nhật `RefundResponse` (preview API) để trả thêm thông tin `serviceFee` của đơn hàng, giúp frontend hiển thị minh bạch.
* **`BookingRepository.java` & `RevenueServiceImpl.java`**:
  * Refactor 3 câu truy vấn JPQL (`getOwnerDailyNetRevenue`, `getOwnerVenueNetRevenueBreakdown`, `sumOwnerCurrentMonthNetRevenue`) để tính toán doanh thu thực tế bằng `totalPrice - serviceFee` của từng đơn hàng. Gỡ bỏ hằng số và tham số truyền vào cố định.

### 3. Frontend (Next.js & Tailwind CSS)
* **`frontend/src/lib/utils.ts`**: Thêm hàm helper `calculatePlatformFee` tính 5% (sàn 10k, trần 30k) đồng nhất với backend.
* **`booking/new/page.tsx` & `booking/payment/page.tsx`**: Loại bỏ giá trị cứng `20000` và tính toán động phí dịch vụ hiển thị trên hóa đơn checkout.
* **`owner/bookings/page.tsx` (Popup Hủy & Hoàn Tiền)**:
  * **Sửa lỗi hiển thị**: Hiển thị rõ ràng dòng khấu trừ **"Phí dịch vụ (Không hoàn lại): -X.XXXđ"** khi khách tự hủy đơn để giải thích lý do số tiền trả khách nhỏ hơn tổng tiền thanh toán (tránh hoang mang "Hoàn 100% nhưng số tiền nhận lại bị hụt").
  * **Sửa lỗi responsive**: Thêm `max-h-[90vh] overflow-y-auto` vào `DialogContent` để tránh popup phình to làm che khuất nút xác nhận ở dưới cùng trên các màn hình nhỏ.
  * **Tối ưu trải nghiệm (UX)**: Ẩn bớt ô nhập **"Ghi chú hủy sân"** khi chủ sân chọn **"Sự cố từ phía sân"** (chỉ hiển thị ô **"Bằng chứng sự cố *"** và tự động đồng bộ nội dung này làm lý do hủy) để tránh nhập thông tin trùng lặp.

---

## 🧪 Kết Quả Kiểm Thử (Verification & Testing)
1. **Backend Tests**: Cập nhật mock và chạy thành công **55/55 tests** (`BookingServiceImplTest`, `RefundServiceTest` & `RevenueServiceImplTest`).
2. **Checkstyle**: **0 violations** (`BUILD SUCCESS`).
3. **Frontend Type-Check**: Chạy thành công `tsc --noEmit` không có lỗi compile nào.
4. **Bảo mật**: Kiểm thử xác nhận chặn đứng Owner gọi trực tiếp API cancel của khách (ném đúng `ForbiddenException` và không cập nhật trạng thái đơn).

