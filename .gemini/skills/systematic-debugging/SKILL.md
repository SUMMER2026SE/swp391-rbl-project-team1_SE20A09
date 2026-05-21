# SKILL: Systematic Debugging

## Framework: ROOT CAUSE ANALYSIS

### Bước 1 — Đọc error đúng cách

```
Stack trace đọc từ DƯỚI LÊN:
1. Caused by (bottom) → đây là ROOT CAUSE thực sự
2. Exception chain phía trên → cascade effects
3. Dòng đầu tiên → nơi exception bị caught

Ví dụ:
[LAYER 3] BeanCreationException: Failed to initialize...
  [LAYER 2] FlywayException: Unable to obtain connection...
    [LAYER 1 - ROOT] PSQLException: password authentication failed ← ĐỌC CÁI NÀY TRƯỚC
```

### Bước 2 — Verify assumptions trước khi fix

> "Đừng bao giờ assume — luôn kiểm tra"

```powershell
# Trước khi sửa config DB → kiểm tra DB có chạy không
docker compose ps

# Trước khi debug Spring Boot → kiểm tra port có bị chiếm không
netstat -ano | findstr ":8080"
netstat -ano | findstr ":5432"

# Trước khi debug API → kiểm tra endpoint trực tiếp
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/hello"
```

### Bước 3 — Isolation

Chia nhỏ vấn đề:
1. DB kết nối được không? → Test riêng bằng psql/DBeaver
2. Backend start không? → Health endpoint
3. Frontend nhận được data không? → Network tab / curl
4. Logic đúng không? → Unit test

## Common Bug Patterns trong dự án này

### PostgreSQL Connection Issues

```
Triệu chứng: "password authentication failed"
Nguyên nhân thường gặp:
1. Máy có PostgreSQL local trên port 5432 → Docker dùng port 5433
2. Spring Boot dùng default "changeme" nhưng DB có password khác
3. pg_hba.conf: local = trust, TCP = scram-sha-256 (khác nhau!)

Checklist:
□ Kiểm tra netstat -ano | findstr ":5432" → có 2 process không?
□ Kiểm tra docker compose ps → postgres trên port 5433?
□ application-dev.yml url có port đúng không? (5433)
□ Test kết nối: docker exec sportvenue-postgres psql -U sportvenue_user -d sportvenue
```

### Next.js Config Issues

```
Triệu chứng: "next.config.ts is not supported"
Nguyên nhân: Next.js 14 không support .ts config
Fix: Đổi thành next.config.js, dùng /** @type */ + module.exports
```

### Spring Boot Java Version Mismatch

```
Triệu chứng: "release version 21 not supported" hoặc UnsupportedClassVersionError
Nguyên nhân: pom.xml có <java.version>21</java.version> nhưng JDK máy là 17
Fix: Cài JDK 21 (Temurin) rồi set JAVA_HOME + PATH
1. Tải tại: https://adoptium.net/ → Eclipse Temurin 21 LTS
2. Set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21...
3. mvnw clean → rebuild
```

### Maven Build Issues

```
Triệu chứng: BUILD FAILURE với download errors
Nguyên nhân thường: Network/proxy, corrupted .m2 cache
Fix:
1. Check internet
2. Xóa corrupted artifact: rm ~/.m2/repository/[artifact]
3. retry: mvnw clean install -U (force update)
```

## Debug Tools

### Backend

```powershell
# Live logs
.\mvnw.cmd spring-boot:run "-Dlogging.level.com.sportvenue=DEBUG"

# Test endpoint
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/hello"

# Database
docker exec -it sportvenue-postgres psql -U sportvenue_user -d sportvenue
```

### Frontend

```javascript
// Network requests: Browser DevTools → Network tab
// State: React DevTools
// TypeScript errors: tsc --noEmit (no output, only type check)
```

## Khi bị stuck

1. **Đọc lại error message** — thường có hint rõ ràng
2. **Google exact error message** + thư viện version
3. **Check GitHub Issues** của dependency
4. **Isolate** — comment out code cho đến khi lỗi biến mất
5. **Rollback** — nếu vừa thay đổi gì và bị lỗi, undo lại
