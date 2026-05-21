# SKILL: TypeScript Pro

## Strict Configuration

```json
// tsconfig.json (đã có trong dự án)
{
  "compilerOptions": {
    "strict": true,           // Bật tất cả strict checks
    "noImplicitAny": true,    // Không cho phép any ngầm định
    "strictNullChecks": true, // null và undefined không tự động assign
    "noUncheckedIndexedAccess": true  // array[i] có thể undefined
  }
}
```

## Utility Types hay dùng

```typescript
// Partial — tất cả optional (update forms)
type UpdateStadiumRequest = Partial<CreateStadiumRequest>;

// Required — tất cả required
type FullStadium = Required<Stadium>;

// Pick — chọn một số fields
type StadiumCard = Pick<Stadium, 'stadiumId' | 'stadiumName' | 'address' | 'pricePerHour'>;

// Omit — bỏ một số fields
type CreateStadiumDto = Omit<Stadium, 'stadiumId' | 'createdAt' | 'averageRating'>;

// Record — map type
type BookingStatusCount = Record<BookingStatus, number>;

// Extract / Exclude — manipulate union types
type ActiveStatus = Extract<StadiumStatus, 'Available'>;  // chỉ 'Available'
type InactiveStatus = Exclude<StadiumStatus, 'Available'>; // 'Maintenance' | 'Closed'
```

## API Response Types

```typescript
// Generic response wrapper (mirror từ backend)
interface ApiResponse<T> {
  data: T;
  message: string;
  timestamp: string;
}

interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // current page
}

// Usage
type StadiumListResponse = PaginatedResponse<Stadium>;
type LoginResponse = ApiResponse<{ user: User; accessToken: string }>;
```

## Type Guards

```typescript
// Runtime type checking
function isApiError(error: unknown): error is { message: string; status: number } {
  return typeof error === 'object' && error !== null
    && 'message' in error && 'status' in error;
}

// Usage
try {
  await api.post('/bookings', data);
} catch (error) {
  if (isApiError(error)) {
    toast.error(error.message);
  }
}
```

## Discriminated Unions (domain modeling)

```typescript
type BookingStatus = 'Pending' | 'Confirmed' | 'Completed' | 'Cancelled';

interface BaseBooking {
  bookingId: number;
  stadiumName: string;
  bookingDate: string;
}

type Booking =
  | (BaseBooking & { bookingStatus: 'Pending'; paymentDeadline: string })
  | (BaseBooking & { bookingStatus: 'Confirmed'; confirmTime: string })
  | (BaseBooking & { bookingStatus: 'Completed'; reviewId?: number })
  | (BaseBooking & { bookingStatus: 'Cancelled'; cancelReason: string });

// TypeScript narrowing
function getBookingAction(booking: Booking) {
  switch (booking.bookingStatus) {
    case 'Pending':
      return `Thanh toán trước ${booking.paymentDeadline}`;
    case 'Confirmed':
      return `Đã xác nhận lúc ${booking.confirmTime}`;
    case 'Cancelled':
      return `Đã hủy: ${booking.cancelReason}`;
  }
}
```

## Tránh common mistakes

```typescript
// ❌ Không dùng any
const data: any = response.data;

// ✅ Dùng unknown + type guard
const data: unknown = response.data;
if (isStadium(data)) { /* sử dụng data */ }

// ❌ Không non-null assertion tùy tiện
const name = user!.fullName;

// ✅ Optional chaining + nullish coalescing
const name = user?.fullName ?? 'Anonymous';

// ❌ Không bỏ qua TypeScript errors bằng @ts-ignore
// @ts-ignore
doSomething(wrongType);

// ✅ Fix đúng type hoặc dùng @ts-expect-error với comment
// @ts-expect-error: Library type definition is incorrect, filed issue #123
doSomething(wrongType);
```
