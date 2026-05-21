# Backend Guardian — Spring Boot Security & Performance

## Vai trò

Bảo vệ backend Java khỏi các lỗi phổ biến: security vulnerabilities, N+1 queries, memory leaks, và architectural anti-patterns.

## Activation

Kích hoạt khi:
- Review code backend Java
- Implement security features (Auth, RBAC)
- Tối ưu performance queries
- Thiết kế API endpoints

## Security Checklist

### Authentication & Authorization
```java
// ✅ Luôn verify JWT signature
// ✅ Check token expiry
// ✅ Validate user có quyền với resource (authorization, không chỉ authentication)
// ✅ Dùng @PreAuthorize cho method-level security
@PreAuthorize("hasRole('OWNER') and @stadiumSecurity.isOwner(authentication, #stadiumId)")
public StadiumDto updateStadium(@PathVariable Integer stadiumId, ...) {}
```

### Input Validation
```java
// ✅ Luôn @Valid ở controller
// ✅ Custom validator cho business rules
// ✅ Sanitize text input (XSS prevention)
@NotBlank @Size(max = 100)
private String stadiumName;

@Positive
private BigDecimal pricePerHour;
```

### SQL Injection Prevention
```java
// ✅ Spring Data JPA — tự động parameterized
stadiumRepo.findByOwnerId(ownerId);

// ✅ JPQL với named params
@Query("SELECT s FROM Stadium s WHERE s.stadiumName LIKE %:name%")
List<Stadium> searchByName(@Param("name") String name);

// ❌ Không concat string vào query
"SELECT * FROM stadiums WHERE name = '" + name + "'"  // SQL Injection!
```

## Performance Checklist

### N+1 Query Prevention
```java
// ❌ Gây N+1: load owner cho mỗi stadium
List<Stadium> stadiums = stadiumRepo.findAll();
stadiums.forEach(s -> System.out.println(s.getOwner().getBusinessName())); // N+1!

// ✅ Fetch JOIN
@Query("SELECT s FROM Stadium s LEFT JOIN FETCH s.owner WHERE s.stadiumStatus = :status")
List<Stadium> findByStatusWithOwner(@Param("status") String status);

// ✅ Hoặc dùng @EntityGraph
@EntityGraph(attributePaths = {"owner", "sportType"})
List<Stadium> findAll();
```

### Lazy Loading Default
```java
// ✅ Mặc định LAZY cho tất cả associations
@ManyToOne(fetch = FetchType.LAZY)  // default phải là LAZY
@OneToMany(fetch = FetchType.LAZY)

// Chỉ EAGER khi thực sự cần (rất hiếm)
```

### Pagination bắt buộc cho list endpoints
```java
// ✅ Luôn paginate, không return toàn bộ list
public Page<StadiumDto> findAll(Pageable pageable) { }

// ❌ Không trả về toàn bộ database table
public List<Stadium> findAll() { }  // Có thể trả về 100k records!
```

## API Design Standards

```java
// ✅ RESTful resource naming (noun, plural)
GET    /api/v1/stadiums          // list
GET    /api/v1/stadiums/{id}     // detail
POST   /api/v1/stadiums          // create
PUT    /api/v1/stadiums/{id}     // full update
PATCH  /api/v1/stadiums/{id}     // partial update
DELETE /api/v1/stadiums/{id}     // delete

// ✅ Versioning trong URL
/api/v1/...

// ✅ Consistent response wrapper
{ "data": {...}, "message": "Success", "timestamp": "..." }

// ✅ Proper HTTP status codes
200 OK, 201 Created, 204 No Content
400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found
422 Unprocessable Entity (validation errors)
500 Internal Server Error
```
