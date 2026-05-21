# SKILL: Spring Boot 3.3 — SportVenue Backend

## Core Configuration Patterns

### application.yml structure

```yaml
spring:
  application:
    name: sport-venue-backend
  datasource:
    url: jdbc:postgresql://localhost:5433/sportvenue
    username: sportvenue_user
    password: changeme
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate        # Flyway quản lý schema
    show-sql: false             # true chỉ trong dev
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### Service Layer Pattern

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)  // default read-only, override khi write
public class StadiumService {

    private final StadiumRepository stadiumRepository;
    private final StadiumMapper stadiumMapper;

    public Page<StadiumSummaryDto> findAll(StadiumFilterRequest filter, Pageable pageable) {
        log.info("Fetching stadiums with filter: {}", filter);
        return stadiumRepository.findAllWithFilters(filter, pageable)
            .map(stadiumMapper::toSummaryDto);
    }

    @Transactional  // write operation
    public StadiumDto create(CreateStadiumRequest request, Integer ownerId) {
        Stadium stadium = stadiumMapper.toEntity(request);
        stadium.setOwner(ownerRepository.getReferenceById(ownerId));
        Stadium saved = stadiumRepository.save(stadium);
        log.info("Created stadium id={} for owner={}", saved.getStadiumId(), ownerId);
        return stadiumMapper.toDto(saved);
    }

    public StadiumDto findById(Integer id) {
        return stadiumRepository.findById(id)
            .map(stadiumMapper::toDto)
            .orElseThrow(() -> new ResourceNotFoundException("Stadium not found: " + id));
    }
}
```

### Repository Pattern

```java
@Repository
public interface StadiumRepository extends JpaRepository<Stadium, Integer> {

    // Spring Data method query
    List<Stadium> findByOwnerOwnerIdAndStadiumStatus(Integer ownerId, String status);

    // JPQL với fetch join (tránh N+1)
    @Query("""
        SELECT s FROM Stadium s
        LEFT JOIN FETCH s.owner o
        LEFT JOIN FETCH s.sportType st
        WHERE (:status IS NULL OR s.stadiumStatus = :status)
        AND (:sportTypeId IS NULL OR st.sportTypeId = :sportTypeId)
    """)
    Page<Stadium> findAllWithFilters(
        @Param("status") String status,
        @Param("sportTypeId") Integer sportTypeId,
        Pageable pageable
    );

    // Tính average rating sau khi có review mới
    @Modifying
    @Query("UPDATE Stadium s SET s.averageRating = " +
           "(SELECT COALESCE(AVG(r.ratingScore), 5.0) FROM Review r WHERE r.stadium.stadiumId = :id) " +
           "WHERE s.stadiumId = :id")
    void recalculateRating(@Param("id") Integer stadiumId);
}
```

### DTO Pattern với MapStruct

```java
// Request DTO
public record CreateStadiumRequest(
    @NotBlank @Size(max = 100) String stadiumName,
    @NotBlank String address,
    @NotNull @Positive BigDecimal pricePerHour,
    @NotNull Integer sportTypeId
) {}

// Response DTO
public record StadiumDto(
    Integer stadiumId,
    String stadiumName,
    String address,
    BigDecimal pricePerHour,
    String stadiumStatus,
    BigDecimal averageRating,
    String sportName,
    String ownerBusinessName
) {}

// MapStruct mapper
@Mapper(componentModel = "spring")
public interface StadiumMapper {
    @Mapping(target = "sportName", source = "sportType.sportName")
    @Mapping(target = "ownerBusinessName", source = "owner.businessName")
    StadiumDto toDto(Stadium stadium);

    @Mapping(target = "stadiumId", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "sportType", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Stadium toEntity(CreateStadiumRequest request);
}
```

## Flyway Migration Conventions

```
db/migration/
├── V1__init.sql              ← Initial schema (đã tạo)
├── V2__add_indexes.sql       ← Thêm indexes
├── V3__seed_data.sql         ← Data mẫu
└── V4__alter_bookings.sql    ← Alter table

Naming: V{number}__{description}.sql
        V = versioned, số tăng dần, description dùng snake_case
```

## Common Gotchas

- `@Transactional` ở **Service**, không Controller
- `FetchType.LAZY` mặc định cho mọi association
- Không expose **Entity** trực tiếp — luôn dùng DTO
- `@GeneratedValue(strategy = IDENTITY)` — không AUTO (dùng cho SERIAL PostgreSQL)
- Hibernate dialect warning có thể bỏ qua hoặc remove `hibernate.dialect` property
