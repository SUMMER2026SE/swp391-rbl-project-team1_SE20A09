# Pull Request Review — fix/cash-payment-status-tracking

> **Verdict:** ✅ Approve (Đã phê duyệt - Sẵn sàng merge vào `main`)
>
> **Mục tiêu PR:** Giải quyết 2 lỗ hổng P0 liên quan đến hệ thống hủy đơn & hoàn tiền (Mục 1.2 và 1.5 trong `docs/qa_findings_refactor_plan.md`), sửa 2 lỗi tự gây ra trong quá trình implement (lỗi độ dài cột DB và thiếu nút hủy đơn cho chủ sân), và bổ sung hiển thị chi tiết "Giá sân" / "Phí dịch vụ" trên UI.

---

## 📋 Tóm Tắt Kết Quả Đánh Giá (Summary of Findings)

| Phân loại | ID | Vị trí | Mô tả thay đổi | Đánh giá chất lượng |
|:---|:---|:---|:---|:---|
| 🟢 **FEAT** | `FEAT-01` | `BookingServiceImpl.java:309` | Confirm payment tiền mặt giờ set `AWAITING_CASH_PAYMENT` (thay vì `PAID`), đồng thời tạo dòng Payment `PENDING` đóng vai trò ledger anchor. | **Xuất sắc.** Giải quyết triệt để lỗi báo PAID giả, giúp hệ thống không bị ngộ nhận doanh thu/hoàn tiền ảo của đơn cash. |
| 🔴 **BUG** | `BUG-01` | `BookingServiceImpl.java:428` | Thắt chặt `validateCancellation` ở Backend: Chặn Owner hủy thẳng các đơn đã thanh toán (`PAID` hoặc `DEPOSITED`) qua API cancel của Customer. | **Xuất sắc.** Ngăn chặn hành vi lách luật hoàn tiền phân tầng của Owner ở tầng API. |
| 🟢 **FEAT** | `FEAT-02` | `owner/bookings/page.tsx:353` | Bổ sung nút "Hủy đơn" riêng cho chủ sân đối với đơn hàng `AWAITING_CASH_PAYMENT` (hủy trực tiếp không hoàn tiền, không preview). | **Tốt.** Khắc phục hoàn hảo việc chủ sân bị "mất nút" hành động do đơn cash chưa được thanh toán thật. |
| 🟢 **FEAT** | `FEAT-03` | `booking/[id]/page.tsx:284` | Trả thêm trường `serviceFee` từ Backend DTO và cập nhật frontend để tách biệt rõ ràng "Giá sân" và "Phí dịch vụ" thay vì hardcode 0đ. | **Đạt yêu cầu.** Hiển thị chính xác, minh bạch hóa đơn đặt sân. |
| 🔴 **BUG** | `BUG-02` | `V102` & `V103` Migrations | Mở rộng check constraint của `payment_status` đồng thời nới rộng kích thước cột `payment_status` từ `VARCHAR(20)` lên `VARCHAR(30)`. | **Chuẩn.** Sử dụng migration nối tiếp V103 thay vì sửa đè V102, bảo vệ toàn vẹn checksum lịch sử của Flyway. |

---

## 🔍 Đánh Giá Sâu Chi Tiết Triển Khai

### 1. Luồng Thanh Toán Tiền Mặt (`AWAITING_CASH_PAYMENT`)
* **Trước refactor:** Khi khách chọn thanh toán tiền mặt, hệ thống cập nhật `PaymentStatus.PAID` ngay lập tức. Điều này dẫn tới hai hệ quả xấu:
  1. Nếu khách tự hủy, hệ thống tự động ghi nhận hoàn tiền ảo.
  2. Báo cáo tài chính bị tính khống (do cộng gộp cả các đơn cash chưa chơi vào doanh thu đã thu thực tế).
* **Sau refactor:** `AWAITING_CASH_PAYMENT` phản ánh đúng bản chất "khách hẹn trả tiền tại sân". Một dòng Payment giả lập `PENDING` được chèn vào DB, đóng vai trò như một mỏ neo (anchor) ghi sổ. Nhờ vậy, logic tính toán hoàn tiền không bị crash vì thiếu dòng Payment gốc, đồng thời hệ thống hiểu đúng đây là đơn chưa thu tiền thật.

### 2. Chống lách luật hoàn tiền của Owner
* Cờ `isVenueOwner` trong `validateCancellation()` đã được ràng buộc chặt chẽ hơn:
  ```java
  boolean wasReallyPaid = booking.getPaymentStatus() == PaymentStatus.PAID
          || booking.getPaymentStatus() == PaymentStatus.DEPOSITED;
  if (isVenueOwner && !isCustomer && wasReallyPaid) {
      throw new ForbiddenException("Đơn đã thanh toán — vui lòng dùng chức năng...");
  }
  ```
* Thiết kế này rất khéo léo:
  * Khách hàng (Customer) vẫn được tự hủy đơn bình thường theo chính sách.
  * Chủ sân (Owner) **bị chặn cứng** ở API cancel chung nếu đơn đã có giao dịch thật, ép buộc họ phải sử dụng API hoàn tiền chuyên dụng để chạy qua chính sách phạt theo giờ (tiering 12h/24h) và tải lên bằng chứng nếu do lỗi của sân (`OWNER_FAULT`).
  * Chủ sân vẫn được phép hủy các đơn chưa thu tiền thật (`UNPAID` hoặc `AWAITING_CASH_PAYMENT`) qua API cancel, tương ứng với nút "Hủy đơn" nhanh trên giao diện mới.

---

## 🧪 Kết Quả Kiểm Thử (Verification Results)

1. **Checkstyle:**
   * Chạy lệnh `mvnw.cmd checkstyle:check` đạt kết quả **0 violations** (Hoàn toàn sạch lỗi format).
2. **Backend Unit Tests:**
   * Lớp test `BookingServiceImplTest` chạy thành công **33/33** test cases.
   * Đã bổ sung đầy đủ 4 test cases kiểm thử hành vi mới: cash confirm tạo đúng Payment PENDING, chặn Owner bypass khi đơn ở trạng thái PAID và DEPOSITED.
3. **Frontend Compilation:**
   * Chạy `npm run type-check` (`tsc --noEmit`) hoàn thành xuất sắc, không có bất kỳ lỗi kiểu dữ liệu nào.

---

## 🏁 Kết Luận

Mã nguồn trong nhánh `fix/cash-payment-status-tracking` có chất lượng rất cao, giải quyết trọn vẹn cả yêu cầu bảo mật ở Backend lẫn trải nghiệm trực quan trên Frontend.

Khuyến nghị **Approve và Merge trực tiếp** nhánh này vào `main` trên GitHub!
