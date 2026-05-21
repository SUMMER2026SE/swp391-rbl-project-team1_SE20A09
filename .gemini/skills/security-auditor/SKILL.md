# SKILL: Security Auditor

## OWASP Top 10 — Spring Boot Checklist

### A01: Broken Access Control
```java
// ❌ Lỗi: User có thể xem booking của người khác
@GetMapping("/bookings/{id}")
public BookingDto getBooking(@PathVariable Integer id) {
    return bookingService.findById(id); // Không check ownership!
}

// ✅ Fix: Verify ownership
@GetMapping("/bookings/{id}")
public BookingDto getBooking(@PathVariable Integer id,
                              @AuthenticationPrincipal UserDetails user) {
    Booking booking = bookingService.findById(id);
    if (!booking.getUser().getEmail().equals(user.getUsername())) {
        throw new AccessDeniedException("Access denied");
    }
    return bookingMapper.toDto(booking);
}
```

### A02: Cryptographic Failures
```java
// ❌ Plain text password
user.setPassword(request.getPassword());

// ✅ BCrypt
user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

// ❌ Weak JWT secret
jwt.secret=secret123

// ✅ Strong secret (>= 256-bit)
jwt.secret=ThisIsAVeryLongSecretKeyForJWTSigningThatIsAtLeast256BitsLong!
```

### A03: Injection
```java
// ❌ SQL Injection risk
@Query("SELECT * FROM users WHERE email = '" + email + "'")

// ✅ Parameterized
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);
```

### A07: Identification & Authentication
```java
// ✅ Rate limiting cho login endpoint
// Dùng Bucket4j hoặc Spring Security's brute force protection

// ✅ JWT expiry ngắn (15 min access, 7 days refresh)
// ✅ Invalidate refresh token sau khi dùng (one-time use)
// ✅ Không lưu accessToken trong localStorage nếu sensitive
//    Dùng httpOnly cookie thay
```

## Security Headers

```java
// SecurityConfig — thêm security headers
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .headers(headers -> headers
            .contentTypeOptions(withDefaults())
            .frameOptions(FrameOptionsConfig::deny)
            .xssProtection(withDefaults())
            .referrerPolicy(policy ->
                policy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN))
        )
        // ...
}
```

## Sensitive Data

```java
// ❌ Không log sensitive info
log.info("User login: email={}, password={}", email, password);

// ✅ Chỉ log non-sensitive
log.info("User login attempt: email={}", email);

// ❌ Không expose stacktrace trong response
{ "error": "NullPointerException at line 234 in UserService..." }

// ✅ Generic error message
{ "message": "An internal error occurred", "code": "INTERNAL_ERROR" }
```

## Frontend Security

```typescript
// ✅ Không store sensitive data trong localStorage
// Dùng memory (Zustand) cho accessToken
// Dùng httpOnly cookie cho refreshToken

// ✅ Sanitize user input trước khi render
import DOMPurify from 'dompurify';
<div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(userContent) }} />

// ✅ Validate trước khi gửi API
const schema = z.object({
  stadiumName: z.string().min(3).max(100),
  pricePerHour: z.number().positive().max(1000000),
});
```

## CORS Configuration

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    // Dev: localhost:3000
    // Prod: chỉ domain production
    config.setAllowedOriginPatterns(List.of(
        "http://localhost:3000",
        "https://sportvenue.vercel.app"
    ));

    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```
