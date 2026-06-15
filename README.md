# 🏟️ Hệ Thống Quản Lý và Cho Thuê Sân Thể Thao

> Nền tảng đặt sân thể thao trực tuyến — tìm kiếm, đặt lịch, thanh toán và kết nối cộng đồng thể thao trong một ứng dụng duy nhất.

---

## 📖 Mục lục

- [Tổng quan dự án](#-tổng-quan-dự-án)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Công nghệ sử dụng](#️-công-nghệ-sử-dụng)
- [Cấu trúc thư mục](#-cấu-trúc-thư-mục-monorepo)
- [Yêu cầu môi trường](#-yêu-cầu-môi-trường)
- [Hướng dẫn khởi chạy](#-hướng-dẫn-khởi-chạy-local-dev)
- [Biến môi trường](#-biến-môi-trường)
- [Cổng dịch vụ](#-cổng-dịch-vụ-sau-khi-khởi-động)
- [Quy ước Git](#-quy-ước-git)
- [Troubleshooting](#-troubleshooting)
- [Thành viên nhóm](#-thành-viên-nhóm)

---

## 📋 Tổng quan dự án

Hệ thống cho phép người dùng **tìm kiếm, đặt lịch và thanh toán** thuê sân thể thao trực tuyến. Tích hợp tính năng mạng xã hội (tìm đối thủ, chat thời gian thực), trợ lý AI và công cụ quản lý toàn diện cho chủ sân.

**4 nhóm người dùng chính:**

| Actor | Mô tả |
|---|---|
| **Guest** | Khách chưa đăng nhập — xem sân, tìm kiếm, xem mạng xã hội |
| **Customer** | Người dùng đã đăng ký — đặt sân, thanh toán, đánh giá, chat |
| **Owner** | Chủ sân — quản lý sân, xác nhận lịch, xem doanh thu |
| **Admin** | Quản trị viên — quản lý toàn hệ thống |

---

## 🏗️ Kiến trúc hệ thống

```
┌─────────────────────────────────────────────────────────┐
│                      Client Layer                        │
│         Next.js 14 (SSR/SSG + App Router)               │
│   Browser  ←→  Socket.io  ←→  TanStack Query            │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTPS / WSS
┌──────────────────────▼──────────────────────────────────┐
│                  Spring Cloud Gateway                    │
│            (Routing · Rate Limiting · CORS)             │
└──────┬──────────────┬──────────────────┬────────────────┘
       │              │                  │
┌──────▼──────┐ ┌─────▼──────┐ ┌────────▼───────┐
│  Auth API   │ │ Booking API│ │  Social/AI API  │
│ :8080/auth  │ │:8080/venues│ │  :8080/social   │
└──────┬──────┘ └─────┬──────┘ └────────┬────────┘
       │              │                  │
┌──────▼──────────────▼──────────────────▼────────────────┐
│                     Data Layer                           │
│   PostgreSQL 16  │  Redis 7  │  MinIO (S3-compatible)   │
└─────────────────────────────────────────────────────────┘
```

**Cơ chế chống double-booking:**
```
User click "Đặt sân"
  → Redis SETNX lock:slot:{slotId}  TTL=300s
      ├─ SETNX = 1 (thành công) → Tiến hành thanh toán
      │     → PostgreSQL SELECT FOR UPDATE (tầng 2)
      │     → Ghi booking → Release lock → Update cache
      └─ SETNX = 0 (thất bại) → Trả lỗi "Slot đang được giữ"
```

---

## 🛠️ Công nghệ sử dụng

### Frontend

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| Next.js | 14 (App Router) | Framework React chính — SSR, SSG, routing |
| TypeScript | 5.x | Type safety, giảm bug runtime |
| Tailwind CSS | 3.x | Utility-first styling, responsive built-in |
| shadcn/ui | Latest | Bộ component UI — Button, Table, Dialog, Calendar |
| Zustand | 4.x | Client state — thông tin user, giỏ đặt sân |
| TanStack Query | 5.x | Server state — fetching, caching, optimistic updates |
| Socket.io-client | 4.x | Real-time — slot trống, chat nhắn tin |

### Backend

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| Spring Boot | 3.3 | Framework lõi, embedded Tomcat |
| Spring Security | 6 | JWT, OAuth2 Google, phân quyền theo role |
| Spring Data JPA | 3.3 | ORM với PostgreSQL, Pessimistic/Optimistic Locking |
| Spring WebSocket | 3.3 | STOMP protocol — real-time slot & chat |
| Spring Cloud Gateway | 4.x | API Gateway, rate limiting, CORS tập trung |
| Spring Mail | 3.3 | Gửi email OTP, thông báo đặt sân, hoàn tiền |
| Flyway | 10.x | Database migration — quản lý version schema |
| Lombok | 1.18.x | Giảm boilerplate — getter/setter/builder tự động |
| MapStruct | 1.5.x | Mapping Entity ↔ DTO tự động |

### Tầng dữ liệu

| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| PostgreSQL | 16 | CSDL chính — ACID, row-level locking, PostGIS |
| Redis | 7 | Cache slot (TTL 30s), distributed lock, session, pub/sub chat |
| MinIO | Latest | Lưu file self-hosted (dev) — tương thích S3 API |
| AWS S3 | — | Lưu file production — ảnh sân, avatar, bài đăng |
| Elasticsearch | 8.x | Tìm kiếm nâng cao, autocomplete *(giai đoạn 2)* |

### Công cụ & DevOps

| Công cụ | Phiên bản | Mục đích |
|---|---|---|
| Docker | 25+ | Containerization |
| Docker Compose | v2 (plugin) | Orchestrate services local |
| GitHub Actions | — | CI/CD — tự động test & build khi push |
| Springdoc OpenAPI | 2.x | Swagger UI — API docs tự động từ annotation |
| Checkstyle | 10.x | Enforce coding standards |
| SonarLint | Latest | Phát hiện bug/code smell trong IDE |
| GitHub Projects | — | Task tracking — Kanban board |

---

## 📁 Cấu trúc thư mục (Monorepo)

```
sport-venue-management/
│
├── 📄 README.md
├── 📄 docker-compose.yml           # Toàn bộ services (infra + app)
├── 📄 docker-compose.dev.yml       # Override cho dev: hot-reload, volume mount
├── 📄 docker-compose.infra.yml     # Chỉ infra: PostgreSQL, Redis, MinIO
├── 📄 .gitignore
├── 📄 .env.example                 # Template tất cả biến môi trường — PHẢI copy thành .env
│
├── 📂 docs/                        # Tài liệu dự án
│   ├── RDS_SportVenue_v1.0.docx    # Requirement & Design Specifications
│   ├── diagrams/                   # Use case diagram, ERD, sequence diagrams
│   └── api/                        # Export Swagger/OpenAPI JSON
│
├── 📂 backend/                     # Spring Boot 3.3 — Java 21
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/sportvenue/
│   │   │   │   ├── config/         # SecurityConfig, RedisConfig, SwaggerConfig, CorsConfig
│   │   │   │   ├── controller/     # REST API controllers (phân theo feature)
│   │   │   │   ├── service/        # Business logic
│   │   │   │   ├── repository/     # Spring Data JPA repositories
│   │   │   │   ├── entity/         # JPA entities — mapping với PostgreSQL tables
│   │   │   │   ├── dto/            # Request/Response DTOs
│   │   │   │   ├── mapper/         # MapStruct: Entity ↔ DTO
│   │   │   │   ├── exception/      # Custom exceptions & @ControllerAdvice handler
│   │   │   │   ├── security/       # JWT filter, OAuth2 success handler
│   │   │   │   ├── websocket/      # STOMP config, chat message handler
│   │   │   │   └── util/           # Helper: format tiền tệ, date, validation
│   │   │   └── resources/
│   │   │       ├── application.yml             # Config chung
│   │   │       ├── application-dev.yml         # Override cho môi trường dev
│   │   │       ├── application-prod.yml        # Override cho môi trường production
│   │   │       └── db/migration/               # Flyway: V1__init.sql, V2__add_venues.sql...
│   │   └── test/                               # Unit tests & Integration tests
│   ├── Dockerfile                              # Multi-stage build: build → runtime
│   ├── .dockerignore
│   ├── mvnw                                    # Maven Wrapper — không cần cài Maven toàn cục
│   ├── mvnw.cmd
│   └── pom.xml
│
├── 📂 frontend/                    # Next.js 14 — Node.js 20
│   ├── src/
│   │   ├── app/
│   │   │   ├── (public)/           # Route công khai — không cần auth
│   │   │   │   ├── page.tsx        # Landing page
│   │   │   │   ├── fields/         # Danh sách sân & tìm kiếm
│   │   │   │   │   └── [id]/       # Chi tiết sân + form đặt
│   │   │   │   └── social/         # Xem bài đăng (read-only)
│   │   │   ├── (auth)/             # Trang xác thực
│   │   │   │   ├── login/
│   │   │   │   └── register/
│   │   │   ├── (user)/             # Route Customer đã đăng nhập
│   │   │   │   ├── bookings/       # Lịch sử đặt sân
│   │   │   │   ├── social/         # Feed + messages/
│   │   │   │   └── profile/        # Trang cá nhân & điểm thưởng
│   │   │   └── (owner)/            # Dashboard Chủ sân
│   │   │       ├── dashboard/      # Tổng quan: đặt sân hôm nay, doanh thu
│   │   │       ├── fields/         # Quản lý sân, [id]/edit
│   │   │       ├── schedule/       # Lịch dạng calendar (FullCalendar)
│   │   │       └── revenue/        # Báo cáo doanh thu + charts
│   │   ├── components/
│   │   │   ├── ui/                 # shadcn/ui base components (copy-paste, không sửa)
│   │   │   ├── booking/            # BookingCalendar.tsx, SlotGrid.tsx
│   │   │   ├── social/             # PostCard.tsx, CommentBox.tsx
│   │   │   └── shared/             # Navbar.tsx, Footer.tsx, LoadingSpinner.tsx
│   │   ├── hooks/
│   │   │   ├── useBookingSlots.ts  # TanStack Query + WebSocket cho slot real-time
│   │   │   ├── useWebSocket.ts     # Kết nối Socket.io
│   │   │   └── useAuth.ts          # Quản lý trạng thái đăng nhập
│   │   ├── lib/
│   │   │   ├── api.ts              # Axios instance, interceptors, auto refresh token
│   │   │   ├── auth.ts             # NextAuth.js config
│   │   │   └── utils.ts            # cn() helper, format VND, format ngày
│   │   └── store/
│   │       ├── authStore.ts        # Zustand: thông tin user hiện tại
│   │       └── bookingStore.ts     # Zustand: trạng thái đặt sân đang thực hiện
│   ├── public/
│   ├── Dockerfile                  # Multi-stage: deps → builder → runner
│   ├── .dockerignore
│   ├── next.config.ts
│   ├── tailwind.config.ts
│   ├── tsconfig.json
│   └── package.json
│
└── 📂 .github/
    ├── workflows/
    │   ├── ci-backend.yml          # Trigger: push/PR → build + test + checkstyle
    │   └── ci-frontend.yml         # Trigger: push/PR → lint + type-check + build
    ├── PULL_REQUEST_TEMPLATE.md
    └── ISSUE_TEMPLATE/
        ├── bug_report.md
        └── feature_request.md
```

---

## ✅ Yêu cầu môi trường

> **Lưu ý:** Kiểm tra version trước khi cài để tránh xung đột.

| Công cụ | Version tối thiểu | Kiểm tra |
|---|---|---|
| Docker Engine | 25.0+ | `docker --version` |
| Docker Compose | v2.20+ (plugin) | `docker compose version` |
| Node.js | 20 LTS | `node --version` |
| JDK | 21 (LTS) | `java -version` |
| Git | 2.40+ | `git --version` |

> ⚠️ Dự án dùng **Docker Compose v2** (`docker compose`, không có dấu `-`). Nếu máy bạn chỉ có v1 (`docker-compose`), hãy cập nhật Docker Desktop hoặc cài plugin riêng.

---

## 🚀 Hướng dẫn khởi chạy (Local Dev)

### Cách 1 — Chạy toàn bộ bằng Docker Compose *(Khuyến nghị)*

Phù hợp khi muốn chạy nhanh, không cần cài JDK hay Node.js trên máy host.

```bash
# 1. Clone repo
git clone https://github.com/<org>/sport-venue-management.git
cd sport-venue-management

# 2. Tạo file .env từ template
cp .env.example .env
# Mở .env và điền các giá trị cần thiết (xem mục Biến môi trường bên dưới)

# 3. Build và khởi động toàn bộ services
docker compose up --build

# Xem log riêng từng service
docker compose logs -f backend
docker compose logs -f frontend
```

### Cách 2 — Chạy riêng từng service *(Dùng khi đang dev tích cực)*

Phù hợp khi cần hot-reload nhanh cho backend hoặc frontend, không muốn rebuild Docker image.

**Bước 1 — Khởi động infrastructure (bắt buộc chạy trước)**

```bash
# Chỉ khởi động PostgreSQL, Redis, MinIO
docker compose -f docker-compose.infra.yml up -d

# Kiểm tra tất cả services đã healthy chưa
docker compose -f docker-compose.infra.yml ps
```

**Bước 2 — Chạy Backend**

```bash
cd backend

./mvnw spring-boot:run 
```

**Bước 3 — Chạy Frontend** *(terminal mới)*

```bash
cd frontend

# Lần đầu: cài dependencies
npm install

# Chạy dev server với hot-reload
npm run dev
```

---

## 🌐 Cổng dịch vụ sau khi khởi động

| Service | URL | Ghi chú |
|---|---|---|
| **Frontend** | http://localhost:3000 | Next.js dev server |
| **Backend API** | http://localhost:8080 | Spring Boot |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API docs, có thể test trực tiếp |
| **API JSON Spec** | http://localhost:8080/v3/api-docs | Export cho Postman/Insomnia |
| **PostgreSQL** | localhost:5432 | DB: `sportvenue`, User: xem `.env` |
| **Redis** | localhost:6379 | Dùng `redis-cli` hoặc RedisInsight để kiểm tra |
| **MinIO Console** | http://localhost:9001 | Quản lý file/bucket trực quan |
| **MinIO API** | http://localhost:9000 | S3-compatible endpoint |

---

## 🔐 Biến môi trường

Copy file `.env.example` thành `.env` và điền các giá trị. **Không commit file `.env` lên Git.**

```dotenv
# ── PostgreSQL ─────────────────────────────────
POSTGRES_DB=sportvenue
POSTGRES_USER=sportvenue_user
POSTGRES_PASSWORD=your_strong_password_here
POSTGRES_PORT=5432

# ── Redis ──────────────────────────────────────
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password_here

# ── MinIO ──────────────────────────────────────
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=your_minio_password_here
MINIO_PORT_API=9000
MINIO_PORT_CONSOLE=9001

# ── Backend (Spring Boot) ──────────────────────
SPRING_PROFILES_ACTIVE=dev
JWT_SECRET=your_jwt_secret_key_min_256_bits
JWT_ACCESS_TOKEN_EXPIRY=900          # giây (15 phút)
JWT_REFRESH_TOKEN_EXPIRY=604800      # giây (7 ngày)

# Google OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# Email SMTP
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password_here

# ── Frontend (Next.js) ─────────────────────────
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_WS_URL=ws://localhost:8080
NEXTAUTH_SECRET=your_nextauth_secret_here
NEXTAUTH_URL=http://localhost:3000
```

> 💡 **Lấy Google Client ID/Secret:** Truy cập [Google Cloud Console](https://console.cloud.google.com/) → APIs & Services → Credentials.

---

## 🌿 Quy ước Git

### Chiến lược nhánh (Git Flow)

```
main ◄─── hotfix/*
  ▲
  │  (merge qua PR, sau khi test staging)
develop ◄─── feature/*
             bugfix/*
             chore/*
```

| Nhánh | Tạo từ | Mục đích | Merge vào |
|---|---|---|---|
| `main` | — | Production-ready, tag version | — |
| `develop` | `main` | Nhánh tích hợp chính | `main` |
| `feature/<scope>/<tên-ngắn>` | `develop` | Phát triển tính năng mới | `develop` |
| `bugfix/<tên-ngắn>` | `develop` | Sửa bug không khẩn cấp | `develop` |
| `hotfix/<tên-ngắn>` | `main` | Sửa bug khẩn cấp trên production | `main` + `develop` |
| `chore/<tên-ngắn>` | `develop` | Cập nhật dependencies, config, CI | `develop` |

**Ví dụ tên nhánh:**
```
feature/auth/google-oauth
feature/booking/recurring-booking
bugfix/slot-cache-not-invalidated
hotfix/payment-callback-500
chore/update-spring-boot-3.3.2
```

### Commit message (Conventional Commits)

Format: `<type>(<scope>): <mô tả ngắn>`

| Type | Khi nào dùng |
|---|---|
| `feat` | Thêm tính năng mới |
| `fix` | Sửa bug |
| `refactor` | Tái cấu trúc code, không thêm tính năng |
| `test` | Thêm/sửa test |
| `docs` | Cập nhật tài liệu, README |
| `chore` | Cập nhật dependencies, config, build script |
| `ci` | Thay đổi GitHub Actions workflow |
| `style` | Format code, không thay đổi logic |
| `perf` | Cải thiện hiệu năng |

**Ví dụ thực tế:**
```
feat(auth): thêm đăng nhập Google OAuth2
feat(booking): implement đặt sân định kỳ hàng tuần
fix(booking): sửa lỗi Redis lock không được release khi thanh toán timeout
fix(slot): cache không bị xóa sau khi hủy đặt sân
refactor(venue): tách VenueService thành VenueQueryService và VenueCommandService
test(auth): thêm integration test cho luồng đăng ký + xác thực OTP
docs: cập nhật hướng dẫn setup môi trường dev
chore: nâng Spring Boot lên 3.3.2
ci: thêm SonarLint check vào CI pipeline
```

### Quy trình làm việc (Pull Request)

```bash
# 1. Luôn pull develop mới nhất trước khi tạo nhánh
git checkout develop
git pull origin develop

# 2. Tạo nhánh mới
git checkout -b feature/booking/recurring-booking

# 3. Code, commit thường xuyên (commit nhỏ, rõ ràng)
git add .
git commit -m "feat(booking): thêm model RecurringBooking và repository"

# 4. Rebase develop trước khi tạo PR (tránh merge conflict)
git fetch origin
git rebase origin/develop

# 5. Push và tạo Pull Request
git push origin feature/booking/recurring-booking
```

**Checklist trước khi tạo PR:**
- [ ] Code build thành công (`./mvnw package -DskipTests` hoặc `npm run build`)
- [ ] Tất cả test pass (`./mvnw test` hoặc `npm run test`)
- [ ] Không có lỗi Checkstyle
- [ ] Đã tự review diff trên GitHub trước khi request review
- [ ] PR description điền đầy đủ (link UC, mô tả thay đổi, cách test)
- [ ] Ít nhất **1 thành viên khác** approve trước khi merge

---

## 🔧 Troubleshooting

### ❌ `docker compose up` báo lỗi port đã bị chiếm

```bash
# Kiểm tra process đang dùng port (ví dụ port 5432)
lsof -i :5432        # macOS/Linux
netstat -ano | findstr :5432   # Windows

# Hoặc đổi port trong .env rồi chạy lại
POSTGRES_PORT=5433
```

### ❌ Backend không kết nối được PostgreSQL

```bash
# Kiểm tra PostgreSQL container đã healthy chưa
docker compose ps

# Xem log chi tiết của postgres
docker compose logs postgres

# Thường gặp: sai POSTGRES_PASSWORD trong .env
# Giải pháp: xóa volume cũ và tạo lại
docker compose down -v
docker compose up -d
```

### ❌ Frontend lỗi `ECONNREFUSED` khi gọi API

Kiểm tra `NEXT_PUBLIC_API_URL` trong `.env` có đúng host và port backend không. Nếu chạy frontend trong Docker, đổi `localhost` thành tên service Docker (`backend`).

### ❌ `./mvnw: Permission denied`

```bash
chmod +x backend/mvnw
```

### ❌ Flyway migration lỗi khi khởi động backend

```bash
# Xem log migration
docker compose logs backend | grep -i flyway

# Nếu cần reset DB dev (MẤT DỮ LIỆU)
docker compose stop backend
docker compose exec postgres psql -U sportvenue_user -d sportvenue -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
docker compose start backend
```

### ❌ Redis connection refused

```bash
# Kiểm tra Redis đang chạy
docker compose exec redis redis-cli ping
# Kết quả mong đợi: PONG

# Nếu Redis cần password, kiểm tra REDIS_PASSWORD trong .env khớp với redis.conf
```

---

## 👥 Thành viên nhóm

| Họ và tên | Vai trò |
|---|---|
| Nguyễn Xuân Huy | Leader |
| Mai Văn Lượng | Member |
| Lý Chí Anh Hào | Member |
| Mai Huy Hoàng | Member |
| Trần Minh An | Member |

---

🔗 Quản lý công việc (Trello)
📌 **Link Trello của nhóm:** https://trello.com/b/muCQpNYN/timsanchothuesan-nhom1-se20a09

🔗 Tài liệu (GG Doc)
📌 https://docs.google.com/document/d/1tBq8FE-IyXy39m8i7qpbuVsPRs1XCz3Cjup7aV7ajYA/edit?tab=t.0#heading=h.lwq4tcwo5ks0
## 📄 License

Dự án này được phát triển cho mục đích học thuật.

