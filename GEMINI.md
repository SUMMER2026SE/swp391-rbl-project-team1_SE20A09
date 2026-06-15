---
trigger: always_on
---

# 🤖 GEMINI.md — Hướng Dẫn Dùng AI Hiệu Quả (SportVenue Team)

> **Đọc file này trước khi prompt AI.** Đây là bản đồ giúp AI hiểu dự án và
> giúp bạn nhận được câu trả lời chính xác, đúng chuẩn dự án ngay từ lần đầu.

---

## 📌 Dự Án Là Gì?

**Sport Venue Management** — Nền tảng đặt sân thể thao trực tuyến.

| Layer | Công nghệ |
|---|---|
| Backend | Spring Boot 3.3, Java 21, JPA, Flyway, Redis |
| Frontend | Next.js 14 (App Router), TypeScript, Tailwind CSS, shadcn/ui |
| Database | PostgreSQL 16 |
| Auth | JWT + Spring Security |

**4 actor:** `Guest` · `Customer` · `Owner` · `Admin`

---

## 🚀 Cách Prompt AI Hiệu Quả

### ✅ Prompt tốt — Cung cấp đủ ngữ cảnh

```
Tôi cần implement UC-OWN-05 (Add new Venue).
Actor: Owner (đã đăng nhập, có owner profile).
Cần: POST /api/v1/stadiums — nhận tên sân, địa chỉ, loại sport, giá/giờ.
Trả về StadiumDto.
Tôi đang làm ở tầng Service.
```

```
Lỗi này xảy ra khi tôi gọi BookingRepository:
[paste stack trace]
Đây là code hiện tại:
[paste code]
```

### ❌ Prompt kém — Quá chung chung

```
Làm sao để tạo sân?       ← AI không biết bạn đang ở tầng nào
Fix lỗi này giúp tôi.     ← Không paste lỗi cụ thể
```

---

## ⚡ Quy Trình Làm Việc Chuẩn

```
[Nhận task UC-OWN-xx] 
    ↓
[Tạo branch từ main]
    git checkout main && git pull
    git checkout -b feature/<scope>/<mô-tả-ngắn>
    ↓
[Implement: Entity → Repository → Service → Controller → DTO]
    ↓
[Test thủ công qua Swagger UI: localhost:8080/swagger-ui.html]
    ↓
[Commit theo Conventional Commits]
    git add . && git commit -m "feat(stadium): implement add new stadium API"
    ↓
[Push và tạo Pull Request → main]
    git push -u origin feature/stadium/add-new
    ↓
[Nhờ 1 thành viên khác review → Merge]
```

---

## 🌿 Git — Quy Tắc Đơn Giản (Dành Cho Người Mới)

### Cấu trúc nhánh

```
main  ← nhánh duy nhất được bảo vệ, KHÔNG push trực tiếp
  ├── feature/stadium/add-new        ← Huy làm
  ├── feature/booking/confirm-reject ← Hoàng làm
  ├── feature/booking/view-list      ← Hoàng làm
  ├── feature/venue/search-filter    ← Hào làm
  └── feature/report/revenue         ← Lượng làm
```

### Đặt tên nhánh

```
feature/<scope>/<mô-tả>    → feature/stadium/crud
fix/<mô-tả-lỗi>            → fix/booking-status-null
chore/<việc>               → chore/add-seed-data
```

### Commit message

```
feat(stadium): implement add new venue API
fix(booking): correct status not updated after confirm
chore(db): add seed data for accessories
docs(api): add Swagger annotation to BookingController
```

### Lệnh Git hay dùng

```bash
# Lấy code mới nhất từ main về
git checkout main && git pull

# Tạo nhánh mới
git checkout -b feature/stadium/add-new

# Commit
git add .
git commit -m "feat(stadium): add Stadium entity and repository"

# Push lần đầu
git push -u origin feature/stadium/add-new

# Push các lần sau
git push

# Xem trạng thái
git status
git log --oneline -5
```

### ❌ Tuyệt Đối Không

- Không `git push` trực tiếp lên `main`
- Không commit file `.env` lên Git
- Không commit thư mục `target/`, `node_modules/`, `.class`

---

## 📐 Quy Tắc Code Bắt Buộc

### Backend (Java)

```java
// ✅ Thứ tự implement đúng chuẩn:
// Entity → Repository → Service Interface → ServiceImpl → Controller → DTO

// ✅ Luôn dùng Lombok
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Stadium { }

// ✅ Controller chỉ nhận/trả DTO, không trả Entity
@GetMapping("/{id}")
public ResponseEntity<StadiumResponse> getById(@PathVariable Integer id) { }

// ✅ Validate input
@PostMapping
public ResponseEntity<StadiumResponse> create(@Valid @RequestBody CreateStadiumRequest req) { }

// ✅ Logging — dùng @Slf4j, không dùng System.out.println
@Slf4j
public class StadiumService {
    log.info("Creating stadium: {}", request.getStadiumName());
}

// ❌ Không dùng
System.out.println("debug");
```

### Frontend (TypeScript)

```typescript
// ✅ Luôn define type — không dùng any
const stadium: Stadium = await api.get<Stadium>('/stadiums/1');

// ✅ Tên component: PascalCase
export function StadiumCard({ stadium }: { stadium: Stadium }) { }

// ✅ Tên hook: camelCase với prefix "use"
export function useStadiums() { }

// ❌ Không dùng
const data: any = await fetch(...);
```

### Cấu Hình & Môi Trường

- **Không sửa đổi cấu hình cá nhân trực tiếp lên file chung:** Các file cấu hình chung của hệ thống (`application.yml`, `application-dev.yml`) chỉ chứa các giá trị fallback dự phòng (như mật khẩu `changeme`).
- **Sử dụng file `.env` cá nhân:** Mọi tùy chỉnh về cổng kết nối, mật khẩu database/redis cá nhân phải được khai báo trong file `.env` cục bộ (file này đã được `.gitignore` chặn, không bao giờ được commit lên).
- **Tránh gây xung đột môi trường:** Việc commit cấu hình cá nhân lên Git sẽ làm hỏng môi trường chạy ứng dụng của các thành viên khác khi pull code mới.

---

## 🗄️ Base Entities Đã Có (Foundation Layer)

Các entity này đã được tạo sẵn — **không tạo lại**, chỉ import và dùng:

```
entity/
├── User.java          ← actor Customer, Owner, Admin
├── Role.java          ← phân quyền
├── Owner.java         ← profile chủ sân (link 1-1 với User)
├── SportType.java     ← Football, Badminton, Basketball...
├── Stadium.java       ← 🔑 entity trung tâm
├── StadiumImage.java  ← ảnh của sân
├── TimeSlot.java      ← khung giờ đặt sân
├── Booking.java       ← đơn đặt sân
├── Payment.java       ← giao dịch thanh toán
├── Review.java        ← đánh giá (có ownerResponse)
├── Complaint.java     ← khiếu nại
└── Accessory.java     ← phụ kiện cho thuê kèm sân

entity/enums/
├── StadiumStatus      → AVAILABLE, MAINTENANCE, CLOSED
├── SlotStatus         → AVAILABLE, BOOKED, MAINTENANCE
├── BookingStatus      → PENDING, CONFIRMED, COMPLETED, CANCELLED
├── PaymentStatus      → UNPAID, PAID, REFUNDED
├── PaymentMethod      → CASH, VNPAY, MOMO, BANKING
├── ComplaintStatus    → OPEN, IN_PROGRESS, RESOLVED
├── NotificationType   → BOOKING, PAYMENT, PROMOTION, SYSTEM
└── ApprovedStatus     → PENDING, APPROVED, REJECTED
```

---

## 🧪 Tài Khoản Test (Seed Data)

| Role | Email | Mật khẩu |
|---|---|---|
| Admin | admin@sportvenue.com | password123 |
| Owner | owner@sportvenue.com | password123 |
| Owner 2 | owner2@sportvenue.com | password123 |
| Customer | customer@sportvenue.com | password123 |
| Customer 2 | customer2@sportvenue.com | password123 |

> Swagger UI: **http://localhost:8080/swagger-ui.html**
> Đăng nhập → lấy JWT token → click Authorize → paste token → test API

---

## 🛡️ Cơ Chế Kiểm Soát Chất Lượng (Quality Gate) & Quy Trình Review
Để đảm bảo mã nguồn đồng bộ và không phát sinh lỗi khi tích hợp hệ thống:
1. **Kiểm duyệt Pull Request (PR):** Tất cả các nhánh feature/fix trước khi merge vào `main` bắt buộc phải được review và Approve bởi các thành viên phụ trách chính (Lượng hoặc Huy). Thành viên sở hữu nhánh tuyệt đối không tự ý merge code của mình khi chưa có sự xác nhận của người duyệt.
2. **Tuân thủ mẫu Code:** Bắt buộc tham chiếu các Controller & DTO mẫu chuẩn Validation (`@Valid`) đã được định nghĩa sẵn. Tuyệt đối không expose Entity trực tiếp ra ngoài Controller.
3. **Quy tắc Routing & UI:** Phải sử dụng chung hệ thống UI (`shadcn/ui` + theme CSS config) để đảm bảo đồng bộ giao diện, không tự ý viết các class CSS tùy tiện phá vỡ bố cục chung.



---

## 📁 Cấu Trúc `.gemini/` (Tham Khảo Thêm)

```
.gemini/
├── rules/
│   ├── coding-guidelines.md   ← quy tắc code chi tiết Java + TypeScript
│   ├── workflow.md             ← Git flow, commit convention
│   ├── language-protocol.md   ← tiếng Anh trong code, tiếng Việt khi giải thích
│   ├── project-context.md     ← tổng quan kiến trúc hệ thống
│   └── design.md              ← nguyên tắc UI/UX
├── agents/
│   ├── reviewer.md            ← dùng khi cần AI review code trước PR
│   ├── backend-guardian.md    ← dùng khi làm API, Security, DB query
│   └── frontend-architect.md  ← dùng khi làm UI component
└── skills/                    ← hướng dẫn kỹ thuật chuyên sâu
```

---

## 🔍 Quy Tắc Review Pull Request (PR)

Khi được yêu cầu review code hoặc PR, AI bắt buộc phải đối chiếu và kiểm tra chi tiết theo các tiêu chí sau:

### 1. Quy trình & Git Flow
- Tên nhánh có đúng format `feature/<scope>/<mô-tả-ngắn>`, `fix/<mô-tả-lỗi>`, `chore/<việc>` không?
- Commit messages có theo chuẩn *Conventional Commits* không?
- Đảm bảo **KHÔNG** commit các file rác hoặc nhạy cảm như `.env`, `target/`, `node_modules/`, `.class`.
- Kiểm tra kỹ xem PR có sửa đổi file cấu hình dùng chung (`application-dev.yml`, `application.yml`, `pom.xml`...) hay không. Đảm bảo **không** chứa cấu hình local hoặc thông tin tài khoản cá nhân.

### 2. Backend (Java / Spring Boot)
- **Security:**
  - Input nhận vào ở Controller bắt buộc phải có `@Valid` + Bean Validation.
  - Tuyệt đối không nối chuỗi trong SQL/JPQL để tránh SQL Injection (phải dùng parameterized/JPA).
  - Kiểm tra xem API đã phân quyền (Authorization) đúng role chưa (Guest, Customer, Owner, Admin).
  - Không log thông tin nhạy cảm (password, token).
- **Performance:**
  - Endpoint lấy danh sách phải có phân trang (`Pageable`).
  - Kiểm tra và tránh lỗi N+1 query (dùng Fetch Join hoặc `@EntityGraph` khi cần).
  - Đảm bảo các trường query chính (như `email`, `stadium_id`, `user_id`) đã được đánh index.
- **Code Quality:**
  - Tuyệt đối không dùng `System.out.println()`, dùng `@Slf4j` và `log.info() / log.error()`.
  - Phân tách rõ ràng DTO với Entity, không expose Entity trực tiếp ra Controller.

### 3. Frontend (TypeScript / Next.js)
- **TypeScript:** Định nghĩa type/interface rõ ràng cho props và API responses. Không lạm dụng kiểu `any`.
- **React/Next.js:**
  - Đầy đủ trạng thái Loading (Skeleton) và Error state thân thiện.
  - Sử dụng key duy nhất và ổn định khi map danh sách.
  - Đảm bảo cleanup các listener/subscribers trong `useEffect` để tránh memory leak.
- **UX/UI:** Form có validation feedback trực quan; UI responsive tốt trên mobile.

### 4. Kiểm tra Ảnh hưởng & Va chạm Hệ thống (System Impact & Conflict Check)
- **Tác động chéo (Cross-module impact):** File thay đổi trong PR có thuộc phạm vi/scope nhiệm vụ của thành viên khác đang phụ trách hay không (đối chiếu với bảng Phân Công Task)? Cần cảnh báo nếu có sự chỉnh sửa chồng chéo.
- **Sửa đổi Core / Base Files:** Nếu PR sửa đổi các file dùng chung (Base Entities trong `entity/`, cấu hình bảo mật `SecurityConfig.java`, file CSS dùng chung `globals.css`, các Util/Helper class), cần phân tích kỹ xem có nguy cơ gây regression (lỗi dây chuyền) ở các module khác không.
- **Xung đột Flyway Migration:** Các file SQL migration mới được thêm vào có bị trùng số phiên bản (ví dụ: hai người cùng tạo file bắt đầu bằng `V8__...`) dẫn đến lỗi migrate khi merge không?
- **Tương thích ngược (Backward Compatibility):** Các thay đổi về cấu trúc API (Request/Response DTO), DB Schema có làm hỏng các phần code hiện tại ở Frontend hoặc Backend chưa kịp cập nhật không?

### 5. Định nghĩa Hoàn thành (Definition of Done - DoD)
- **Có file test:** Code mới/thay đổi phải có các file test đi kèm để kiểm thử tính năng (Unit Test hoặc Integration Test).
- **Chạy pass 100%:** Tất cả các bài test (cũ và mới) phải chạy thành công 100% và không có lỗi compile/build.
- **Code sạch:** Xóa toàn bộ debug thừa (`console.log`, `System.out.println()`, comment nháp/code thừa).

### 6. Định dạng Output Review của AI
AI sẽ xuất kết quả review theo template sau:
- **Verdict:** ✅ Approve | 🔄 Changes Requested | ❌ Reject
- Phân loại rõ từng lỗi/nhận xét:
  - `[BUG]`: Lỗi logic, gây crash hoặc chạy sai nghiệp vụ (Bắt buộc sửa).
  - `[SECURITY]`: Lỗ hổng bảo mật (Bắt buộc sửa).
  - `[PERF]`: Vấn đề hiệu năng, N+1 query, render chậm (Bắt buộc sửa).
  - `[STYLE]`: Vi phạm quy chuẩn code, format (Nên sửa).
  - `[SUGGEST]`: Gợi ý cải tiến không bắt buộc.

---

## 🚨 Nguyên Tắc Tuyệt Đối

1. **KHÔNG** commit `.env`, credentials, API key lên Git
2. **KHÔNG** push thẳng lên `main` — luôn qua Pull Request
3. **KHÔNG** tạo lại Entity đã có — import từ `entity/` package
4. **LUÔN** tạo Flyway migration khi thay đổi schema DB
5. **LUÔN** validate input bằng `@Valid` ở Controller
6. **LUÔN** dùng DTO — không expose Entity trực tiếp ra API
7. **KHÔNG** tự ý thay đổi các tham số cấu hình dùng chung (cổng, tài khoản, mật khẩu mặc định) trong các file cấu hình (`application-dev.yml`...) lên Git.
