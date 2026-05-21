# Project Context — Sport Venue Management System

## Tổng quan

Hệ thống cho phép người dùng **tìm kiếm, đặt lịch và thanh toán** thuê sân thể thao trực tuyến.
Tích hợp tính năng mạng xã hội (tìm đối thủ, chat thời gian thực), hệ thống điểm thưởng và quản lý toàn diện cho chủ sân.

**Môn học:** SWP391 — Software Project  
**Học kỳ:** Summer 2026 — SE20A09  
**Repository:** `swp391-rbl-project-team1_SE20A09`

## Actors

| Actor | Vai trò |
|---|---|
| **Guest** | Xem sân, tìm kiếm, xem posts |
| **Customer** | Đặt sân, thanh toán, đánh giá, chat, tích điểm |
| **Owner** | Quản lý sân, xác nhận lịch, xem doanh thu |
| **Admin** | Quản lý toàn hệ thống, duyệt owner |

## Tech Stack

### Backend
- **Runtime:** Java 21 (Temurin LTS)
- **Framework:** Spring Boot 3.3.0
- **ORM:** Spring Data JPA (Hibernate 6.5)
- **Migration:** Flyway 10.x
- **Security:** Spring Security 6 (JWT skeleton — chưa implement)
- **Cache:** Spring Data Redis
- **Docs:** SpringDoc OpenAPI (Swagger UI: `/swagger-ui.html`)
- **Build:** Maven (Wrapper `mvnw.cmd`)
- **Package:** `com.sportvenue`

### Frontend
- **Framework:** Next.js 14.2.3 (App Router)
- **Language:** TypeScript strict
- **Styling:** Tailwind CSS v3 + shadcn/ui (chưa init)
- **State:** Zustand + TanStack Query
- **HTTP:** Axios với JWT interceptor
- **Config:** `next.config.js` (**không phải `.ts`** — Next.js 14 không hỗ trợ)

### Infrastructure (Docker)
- **Database:** PostgreSQL 16-alpine → port **5433** (tránh conflict với PG local)
- **Cache:** Redis 7-alpine → port 6379
- **Storage:** MinIO → port 9000/9001
- **Note:** Máy dev có PostgreSQL Windows local trên port 5432

## Database Schema

18 bảng chính:
```
roles, sport_types, users, owners, stadiums, stadium_images,
time_slots, bookings, payments, reviews, promotions, booking_promotions,
notifications, conversations, conversation_members, messages, posts, comments
```

**Primary Key strategy:** `SERIAL` (INT auto-increment) — *không dùng UUID*

## Cổng dịch vụ

| Service | Port | URL |
|---|---|---|
| Frontend | 3000 | http://localhost:3000 |
| Backend | 8080 | http://localhost:8080 |
| Swagger | 8080 | http://localhost:8080/swagger-ui.html |
| PostgreSQL | **5433** | jdbc:postgresql://localhost:5433/sportvenue |
| Redis | 6379 | localhost:6379 |
| MinIO Console | 9001 | http://localhost:9001 |

## Khởi động dev

```powershell
# Terminal 1 — Infrastructure
docker compose -f docker-compose.infra.yml up -d

# Terminal 2 — Backend  
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"

# Terminal 3 — Frontend
cd frontend
npm run dev
```

## Trạng thái hiện tại

- ✅ Database schema (V1__init.sql) — 18 bảng
- ✅ Backend skeleton — HealthController, SecurityConfig, SwaggerConfig
- ✅ Frontend skeleton — Landing page, dark theme
- ✅ CI/CD — GitHub Actions (backend + frontend)
- ✅ Docker infrastructure — PostgreSQL + Redis + MinIO
- ⏳ **Chưa implement:** JWT filter, Auth service, tất cả business features
