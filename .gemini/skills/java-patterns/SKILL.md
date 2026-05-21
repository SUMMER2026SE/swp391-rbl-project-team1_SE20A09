# SKILL: Java 21 Patterns

## Modern Java 21 Features (LTS)

### Records (DTO replacement)

```java
// ✅ Records cho immutable DTOs
public record CreateBookingRequest(
    @NotNull Integer stadiumId,
    @NotNull Integer timeSlotId,
    @NotNull LocalDate bookingDate,
    String note
) {}

public record BookingDto(
    Integer bookingId,
    String stadiumName,
    LocalDate bookingDate,
    LocalTime startTime,
    LocalTime endTime,
    BigDecimal totalPrice,
    String bookingStatus
) {}

// ✅ Compact constructor cho validation
public record PriceRange(BigDecimal min, BigDecimal max) {
    public PriceRange {
        if (min != null && max != null && min.compareTo(max) > 0)
            throw new IllegalArgumentException("min must be <= max");
    }
}
```

### Sealed Classes (domain modeling)

```java
// Modeling booking payment outcomes
public sealed interface PaymentResult
    permits PaymentResult.Success, PaymentResult.Failed, PaymentResult.Pending {

    record Success(String transactionId, BigDecimal amount) implements PaymentResult {}
    record Failed(String reason, int errorCode) implements PaymentResult {}
    record Pending(String referenceCode) implements PaymentResult {}
}

// Pattern matching với switch
String message = switch (result) {
    case PaymentResult.Success s -> "Thanh toán thành công: " + s.transactionId();
    case PaymentResult.Failed f  -> "Thanh toán thất bại: " + f.reason();
    case PaymentResult.Pending p -> "Đang xử lý: " + p.referenceCode();
};
```

### Text Blocks (SQL, JSON in code)

```java
// ✅ Text blocks cho JPQL dài
@Query("""
    SELECT new com.sportvenue.dto.StadiumSummaryDto(
        s.stadiumId, s.stadiumName, s.address,
        s.pricePerHour, s.averageRating, st.sportName
    )
    FROM Stadium s
    JOIN s.sportType st
    WHERE (:sportTypeId IS NULL OR st.sportTypeId = :sportTypeId)
      AND (:minPrice IS NULL OR s.pricePerHour >= :minPrice)
      AND (:maxPrice IS NULL OR s.pricePerHour <= :maxPrice)
      AND s.stadiumStatus = 'Available'
    ORDER BY s.averageRating DESC
""")
Page<StadiumSummaryDto> searchStadiums(
    @Param("sportTypeId") Integer sportTypeId,
    @Param("minPrice") BigDecimal minPrice,
    @Param("maxPrice") BigDecimal maxPrice,
    Pageable pageable
);
```

### Pattern Matching instanceof

```java
// ✅ Pattern matching
if (exception instanceof ResourceNotFoundException nfe) {
    return ResponseEntity.status(404).body(new ErrorResponse(nfe.getMessage()));
} else if (exception instanceof ValidationException ve) {
    return ResponseEntity.status(400).body(new ErrorResponse(ve.getMessage()));
}
```

### Optional Patterns

```java
// ✅ orElseThrow với supplier
Stadium stadium = stadiumRepo.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Stadium not found: " + id));

// ✅ map + filter
Optional<String> ownerEmail = userRepo.findById(ownerId)
    .filter(u -> u.getAccountStatus().equals("Active"))
    .map(User::getEmail);

// ❌ Anti-pattern
if (optional.isPresent()) {
    doSomething(optional.get()); // Dùng ifPresent() thay
}
optional.get(); // Sẽ throw nếu empty
```

### Stream API

```java
// ✅ Collecting to maps
Map<String, Long> bookingsByStatus = bookings.stream()
    .collect(Collectors.groupingBy(
        Booking::getBookingStatus,
        Collectors.counting()
    ));

// ✅ Flat map
List<String> allImages = stadiums.stream()
    .flatMap(s -> s.getImages().stream())
    .map(StadiumImage::getImageUrl)
    .toList(); // Java 16+ immutable list

// ✅ Parallel stream (chỉ với CPU-bound tasks lớn)
BigDecimal totalRevenue = completedBookings.parallelStream()
    .map(Booking::getTotalPrice)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

## Builder Pattern với Lombok

```java
@Builder
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "bookings")
public class Booking {
    // ... fields
}

// Usage:
Booking booking = Booking.builder()
    .user(user)
    .stadium(stadium)
    .timeSlot(timeSlot)
    .bookingDate(LocalDate.now().plusDays(1))
    .totalPrice(stadium.getPricePerHour())
    .bookingStatus("Pending")
    .build();
```
