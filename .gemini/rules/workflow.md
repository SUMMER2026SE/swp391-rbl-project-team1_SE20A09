# Workflow — Git & Development Process

## Branch Strategy

```
main                    ← production-ready, protected
├── develop             ← integration branch
│   ├── feature/auth-jwt         ← new features
│   ├── feature/stadium-crud
│   ├── feature/booking-flow
│   ├── fix/postgres-port-conflict  ← bug fixes
│   └── chore/update-dependencies   ← maintenance
```

### Branch Naming

```
feature/<ticket-or-description>   → feature/auth-jwt-filter
fix/<what-was-fixed>              → fix/flyway-migration-error
chore/<task>                      → chore/upgrade-spring-boot
docs/<what>                       → docs/api-endpoints
```

## Commit Convention (Conventional Commits)

```
<type>(<scope>): <short description>

Types:
  feat     → tính năng mới
  fix      → sửa lỗi
  chore    → không ảnh hưởng production (deps, config)
  docs     → chỉ documentation
  refactor → refactor không thay đổi behavior
  test     → thêm/sửa tests
  style    → format, whitespace (không đổi logic)
  perf     → cải thiện performance

Scopes: auth, stadium, booking, payment, user, review, chat, social, infra, ci
```

### Ví dụ commit tốt

```
feat(auth): add JWT authentication filter
fix(booking): correct time slot overlap validation
chore(deps): upgrade Spring Boot to 3.3.1
docs(api): add Swagger annotations to StadiumController
refactor(service): extract payment logic to PaymentService
test(stadium): add unit tests for StadiumService
```

## Pull Request Rules

1. **Tên PR** theo format: `feat(auth): Add JWT filter and UserDetailsService`
2. **Mô tả** phải có: Changes, Testing steps, Screenshots (nếu UI)
3. **Minimum 1 reviewer** trước khi merge
4. **CI phải pass** (backend build + frontend lint)
5. **Không merge** nếu có conflict chưa resolve

## Development Flow

```
1. Pull develop mới nhất
   git checkout develop && git pull

2. Tạo branch mới
   git checkout -b feature/my-feature

3. Code + commit thường xuyên (nhỏ, rõ ràng)

4. Push và tạo PR → develop
   git push -u origin feature/my-feature

5. Request review từ teammate

6. Sau khi approve + CI pass → Merge (Squash merge)

7. Xóa branch sau merge
```

## Quy tắc quan trọng

- ❌ **Không push trực tiếp lên `main`**
- ❌ **Không commit file nhạy cảm** (`.env`, credentials)
- ✅ **Commit nhỏ, thường xuyên** — 1 commit = 1 việc
- ✅ **Sync với develop hàng ngày** để tránh conflict lớn
