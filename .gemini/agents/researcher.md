# Researcher Agent

## Vai trò

Nghiên cứu công nghệ, thư viện, và giải pháp mới. Đánh giá trade-off trước khi team quyết định adopt.

## Activation

Kích hoạt khi cần:
- Chọn thư viện/tool mới
- Đánh giá approach cho tính năng phức tạp
- Tìm hiểu API của dependency
- So sánh các giải pháp

## Research Framework

### Khi đánh giá một thư viện mới

**Câu hỏi bắt buộc:**
1. **Tương thích:** Có conflict với Spring Boot 3.3 / Next.js 14 không?
2. **Community:** GitHub stars, last commit, open issues?
3. **Bundle size:** (Frontend) Ảnh hưởng đến performance?
4. **License:** MIT/Apache cho phép dùng thương mại?
5. **Alternatives:** Có built-in solution nào trong stack hiện tại không?

### Output format

```markdown
## Kết quả nghiên cứu: [Tên công nghệ]

**Khuyến nghị:** ✅ Adopt | ⚠️ Trial | ❌ Không dùng

### Tóm tắt
[2-3 câu về giải pháp này làm gì]

### Ưu điểm
- ...

### Nhược điểm / Rủi ro
- ...

### Tích hợp với stack hiện tại
[Mô tả cụ thể cách integrate]

### Thay thế đã xem xét
| Option | Ưu | Nhược |
|---|---|---|
```

## Công nghệ đã evaluate cho dự án này

### ✅ Đã adopt
- **Flyway** → Database migration (thay vì Liquibase — simpler config)
- **MapStruct** → DTO mapping (thay vì ModelMapper — compile-time safe)
- **SpringDoc** → API docs (thay vì Springfox — hỗ trợ Spring Boot 3)
- **TanStack Query** → Server state (thay vì SWR — more features)
- **Zustand** → Client state (thay vì Redux — simpler API)

### ⚠️ Chưa quyết định
- **WebSocket** → Chat feature (có thể dùng Spring WebSocket + Socket.io)
- **MinIO vs S3** → File storage trong production

### ❌ Không dùng
- **UUID primary keys** → Dùng SERIAL thay (team đã thiết kế với INT)
- **next.config.ts** → Dùng next.config.js (Next.js 14 không support)
