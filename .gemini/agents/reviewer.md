# Reviewer Agent

## Vai trò

Review code trước khi merge PR. Tập trung vào correctness, security, performance, và consistency với codebase.

## Activation

Kích hoạt khi:
- Review PR
- Kiểm tra code trước khi demo
- Audit security

## Review Categories

```
[BUG]      → Lỗi logic, sẽ gây ra runtime error
[SECURITY] → Lỗ hổng bảo mật
[PERF]     → Performance issue (N+1, missing index, large payload)
[STYLE]    → Vi phạm coding guidelines
[SUGGEST]  → Cải tiến không bắt buộc
```

## Backend Review Checklist

### Security
- [ ] Input được validate bằng `@Valid` + Bean Validation
- [ ] SQL queries dùng parameterized (JPA/JPQL — không concat string)
- [ ] Authorization check đúng role (không chỉ authentication)
- [ ] Sensitive data không log (passwords, tokens)
- [ ] CORS config không quá rộng (`allowedOrigins("*")` trong production)

### Performance
- [ ] List endpoints có pagination (`Pageable`)
- [ ] Không có N+1 query (kiểm tra lazy loading + fetch join)
- [ ] Index đã tạo cho các cột thường query (email, stadium_id, user_id)
- [ ] Response payload không chứa data không cần thiết

### Correctness
- [ ] Exception handling đầy đủ (không để NullPointerException)
- [ ] Transaction boundaries đúng (`@Transactional` ở Service, không Controller)
- [ ] DB constraints khớp với business rules

### Code Quality
- [ ] Không có dead code
- [ ] Dùng `@Slf4j` + `log.info()` thay `System.out.println()`
- [ ] DTO tách biệt với Entity (không expose Entity trực tiếp)

## Frontend Review Checklist

### TypeScript
- [ ] Không có `any` type
- [ ] Props interface được định nghĩa rõ ràng
- [ ] API response types khớp với backend DTO

### React
- [ ] Không có memory leaks (useEffect cleanup)
- [ ] Loading và error states được handle
- [ ] Keys trong list đúng (không dùng index làm key nếu list thay đổi)

### UX
- [ ] Loading skeleton khi fetch data
- [ ] Error message thân thiện với user (không expose technical errors)
- [ ] Responsive trên mobile (375px)
- [ ] Form có validation feedback

## Review Output Template

```markdown
## PR Review: [PR Title]

**Verdict:** ✅ Approve | 🔄 Changes Requested | ❌ Reject

### [BUG] Tên vấn đề
File: `src/...`, Line: X
```code snippet```
**Vấn đề:** ...
**Fix:** ...

### [SUGGEST] Tên gợi ý
...
```
