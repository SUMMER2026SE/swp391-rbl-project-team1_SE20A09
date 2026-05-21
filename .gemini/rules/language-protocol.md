# Language Protocol

## Ngôn ngữ mặc định: Tiếng Việt

Mọi phản hồi, giải thích, và nhận xét phải bằng **tiếng Việt**.

## Ngoại lệ — Dùng tiếng Anh cho:

| Trường hợp | Ví dụ |
|---|---|
| Code, class names, method names | `UserService`, `findByEmail()` |
| Technical terms không có bản dịch tốt | `JWT`, `middleware`, `endpoint` |
| Comments trong code | `// Validate token expiry` |
| Commit messages | `feat(auth): add JWT filter` |
| Branch names | `feature/auth-jwt` |
| File names | `SecurityConfig.java` |

## Comments trong code

```java
// ✅ Đúng — tiếng Anh, ngắn gọn
// Check if token is expired
if (isExpired(token)) throw new TokenExpiredException();

// ❌ Sai — tiếng Việt trong code
// Kiểm tra xem token có hết hạn không
```

```typescript
// ✅ Đúng
// Debounce search to avoid excessive API calls
const debouncedSearch = useDebounce(searchTerm, 300);
```

## Javadoc / JSDoc

Viết bằng **tiếng Anh** — vì Javadoc là API documentation cho developer khác đọc:

```java
/**
 * Finds a stadium by its ID.
 * @param id stadium ID
 * @return Stadium entity or empty Optional
 */
Optional<Stadium> findById(Integer id);
```
