# Coding Guidelines

## Java (Backend)

### Naming Conventions

```java
// Classes: PascalCase
public class StadiumService { }
public class BookingController { }

// Methods & variables: camelCase  
public List<Stadium> findAvailableStadiums() { }
private Integer maxCapacity;

// Constants: UPPER_SNAKE_CASE
public static final String JWT_PREFIX = "Bearer ";

// Packages: lowercase
package com.sportvenue.service;
package com.sportvenue.controller;
```

### Package Structure

```
com.sportvenue/
├── config/          ← Spring configs (Security, Swagger, Redis)
├── controller/      ← REST controllers (@RestController)
├── service/         ← Business logic (@Service)
├── repository/      ← JPA repos (@Repository)
├── entity/          ← JPA entities (@Entity)
├── dto/             ← Data Transfer Objects (request/response)
├── mapper/          ← MapStruct mappers
├── exception/       ← Custom exceptions + GlobalExceptionHandler
├── security/        ← JWT filter, UserDetailsService
└── util/            ← Utility classes
```

### Entity Rules

```java
// ✅ Dùng SERIAL (IDENTITY), không UUID
@Entity
@Table(name = "stadiums")
public class Stadium {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer stadiumId;

    @Column(name = "stadium_name", nullable = false)
    private String stadiumName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;
}

// ✅ Dùng Lombok để giảm boilerplate
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Stadium { }
```

### Controller Rules

```java
@RestController
@RequestMapping("/api/v1/stadiums")
@RequiredArgsConstructor
@Tag(name = "Stadium", description = "Stadium management APIs")
public class StadiumController {

    private final StadiumService stadiumService;

    // ✅ ResponseEntity với HTTP status rõ ràng
    @GetMapping("/{id}")
    public ResponseEntity<StadiumDto> getStadium(@PathVariable Integer id) {
        return ResponseEntity.ok(stadiumService.findById(id));
    }

    // ✅ @Valid cho request body
    @PostMapping
    public ResponseEntity<StadiumDto> create(@Valid @RequestBody CreateStadiumRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stadiumService.create(request));
    }
}
```

### Error Handling

```java
// ✅ Custom exceptions
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

// ✅ Global handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage()));
    }
}
```

---

## TypeScript (Frontend)

### Naming

```typescript
// Components: PascalCase
export function StadiumCard() { }

// Hooks: camelCase với prefix "use"
export function useStadiumList() { }

// Types/Interfaces: PascalCase
interface Stadium { stadiumId: number; stadiumName: string; }
type BookingStatus = 'Pending' | 'Confirmed' | 'Completed' | 'Cancelled';

// Constants: UPPER_SNAKE_CASE hoặc camelCase
const API_BASE_URL = 'http://localhost:8080';
```

### API Types (mirror từ backend DTO)

```typescript
// src/types/stadium.ts
export interface Stadium {
  stadiumId: number;
  stadiumName: string;
  address: string;
  pricePerHour: number;
  stadiumStatus: 'Available' | 'Maintenance' | 'Closed';
  averageRating: number;
  sportType: SportType;
}

export interface CreateStadiumRequest {
  stadiumName: string;
  address: string;
  pricePerHour: number;
  sportTypeId: number;
}
```

### Không dùng `any`

```typescript
// ❌ Sai
const data: any = await api.get('/stadiums');

// ✅ Đúng
const data: Stadium[] = await api.get<Stadium[]>('/stadiums').then(r => r.data);
```

---

## General Rules

- **Không commit** file `.env`, `target/`, `node_modules/`, `.class`
- **Không dùng** `System.out.println` — dùng `@Slf4j` + `log.info()`
- **Không dùng** `console.log` trong production code — dùng proper logging
- **Luôn validate** input ở cả backend (`@Valid`) và frontend (Zod/react-hook-form)
