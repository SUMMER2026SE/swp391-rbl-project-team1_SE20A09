# Workflow — Git & Development Process

## Branch Strategy (Simplified)

> Team lần đầu làm việc với Git — giữ đơn giản, dễ hiểu.

```
main  ← nhánh duy nhất được bảo vệ
  ├── feature/stadium/add-new         ← tính năng mới
  ├── feature/booking/confirm-reject
  ├── fix/booking-null-status         ← sửa lỗi
  └── chore/add-seed-data             ← maintenance
```

**Quy tắc cốt lõi:** Mọi thay đổi đều đi qua **Pull Request → main**. Không ai được push thẳng lên `main`.

### Branch Naming

```
feature/<scope>/<mô-tả-ngắn>   → feature/stadium/crud
fix/<mô-tả-lỗi>                → fix/booking-status-null
chore/<việc>                   → chore/add-seed-data
docs/<gì>                      → docs/swagger-annotations
```

---

## Commit Convention (Conventional Commits)

```
<type>(<scope>): <mô tả ngắn>

Types:
  feat     → tính năng mới
  fix      → sửa lỗi
  chore    → không ảnh hưởng production (deps, config, seed data)
  docs     → chỉ documentation
  refactor → refactor không thay đổi behavior
  test     → thêm/sửa tests
  style    → format, whitespace (không đổi logic)
  perf     → cải thiện performance

Scopes: auth, stadium, booking, payment, user, review, complaint,
        accessory, report, search, notification, infra, ci
```

### Ví dụ commit tốt

```
feat(stadium): implement add new stadium API
feat(booking): add confirm and reject booking endpoint
fix(slot): slot status not updated after booking confirmed
chore(db): add V6 seed data for venues and bookings
docs(api): add Swagger annotations to StadiumController
refactor(service): extract revenue calculation to RevenueService
test(stadium): add unit tests for StadiumService
```

---

## Development Flow (Từng Bước)

```bash
# 1. Luôn bắt đầu từ main mới nhất
git checkout main
git pull

# 2. Tạo nhánh mới cho task của bạn
git checkout -b feature/stadium/add-new

# 3. Code, commit thường xuyên — mỗi commit 1 việc nhỏ
git add .
git commit -m "feat(stadium): add Stadium entity and repository stub"
git commit -m "feat(stadium): implement StadiumService.create()"
git commit -m "feat(stadium): add POST /api/v1/stadiums endpoint"

# 4. Push lên GitHub
git push -u origin feature/stadium/add-new   # lần đầu
git push                                       # các lần sau

# 5. Vào GitHub → tạo Pull Request → main
# 6. Nhờ 1 teammate review
# 7. Sau khi approve → Merge (Squash merge)
# 8. Xóa branch sau merge
```

---

## Pull Request Rules

1. **Tên PR** theo format: `feat(stadium): Add new venue CRUD API`
2. **Mô tả** phải có: Làm gì, Test thế nào, Link UC liên quan
3. **Minimum 1 reviewer** trước khi merge
4. **CI phải pass** (backend build + frontend lint)
5. **Resolve conflict** trước khi request review

---

## Xử Lý Conflict (Khi Bị Conflict Với Main)

```bash
# Trong nhánh feature của bạn
git fetch origin
git merge origin/main

# Resolve conflict thủ công → sau đó
git add .
git commit -m "chore: resolve merge conflict with main"
git push
```

---

## Quy Tắc Quan Trọng

- ❌ **Không push trực tiếp lên `main`**
- ❌ **Không commit file nhạy cảm** (`.env`, credentials, API keys)
- ❌ **Không commit** `target/`, `node_modules/`, `.class`, `.env`
- ✅ **Commit nhỏ, thường xuyên** — 1 commit = 1 việc cụ thể
- ✅ **Pull main mới nhất** trước khi bắt đầu task mới
- ✅ **Luôn test** qua Swagger trước khi tạo PR
