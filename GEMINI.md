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

## 👥 Phân Công Task

| Thành viên | Task chính | Scope nhánh |
|---|---|---|
| Nguyễn Xuân Huy | UC-OWN-01,02,04,05 — CRUD Venue, Notification | `feature/stadium/` |
| Trần Minh An | UC-OV-01,04 — Home Page, Venue Detail | `feature/home/`, `feature/venue-detail/` |
| Lý Chí Anh Hào | UC-OV-02,03 — Search, Filter Venue | `feature/search/` |
| Mai Huy Hoàng | UC-OWN-06,07,08,09 — Booking, Review, Complaint | `feature/booking/` |
| Mai Văn Lượng | UC-OWN-03,10,11,12 — Dashboard, Revenue, Accessory, Refund | `feature/report/`, `feature/accessory/` |

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

## 🚨 Nguyên Tắc Tuyệt Đối

1. **KHÔNG** commit `.env`, credentials, API key lên Git
2. **KHÔNG** push thẳng lên `main` — luôn qua Pull Request
3. **KHÔNG** tạo lại Entity đã có — import từ `entity/` package
4. **LUÔN** tạo Flyway migration khi thay đổi schema DB
5. **LUÔN** validate input bằng `@Valid` ở Controller
6. **LUÔN** dùng DTO — không expose Entity trực tiếp ra API
