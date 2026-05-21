# Frontend Architect — Next.js 14 + Premium UI

## Vai trò

Thiết kế và xây dựng giao diện người dùng cao cấp, nhất quán với design system đã định nghĩa.
Đảm bảo performance, accessibility, và UX tốt nhất.

## Activation

Kích hoạt khi:
- Tạo pages, components mới
- Thiết kế UI layout
- Tối ưu performance frontend
- Review code TypeScript/React

## Next.js 14 App Router Patterns

### Server vs Client Components

```tsx
// ✅ Server Component (default) — data fetching, SEO
// app/stadiums/page.tsx
export default async function StadiumsPage() {
  const stadiums = await fetchStadiums(); // trực tiếp, không useEffect
  return <StadiumList stadiums={stadiums} />;
}

// ✅ Client Component — interactivity, state
'use client';
export function BookingForm() {
  const [date, setDate] = useState<Date>();
  // ...
}
```

### Folder Structure (App Router)

```
src/app/
├── (public)/               ← Route group — không tạo segment
│   ├── page.tsx            ← Landing page
│   ├── stadiums/
│   │   ├── page.tsx        ← List stadiums
│   │   └── [id]/page.tsx   ← Stadium detail
│   └── posts/page.tsx
├── (auth)/                 ← Auth routes
│   ├── login/page.tsx
│   └── register/page.tsx
├── (dashboard)/            ← Protected routes
│   ├── layout.tsx          ← Dashboard layout với sidebar
│   ├── bookings/page.tsx
│   ├── profile/page.tsx
│   └── owner/
│       ├── stadiums/page.tsx
│       └── analytics/page.tsx
├── layout.tsx              ← Root layout
└── globals.css
```

### Data Fetching Patterns

```tsx
// ✅ TanStack Query cho client-side với cache
function useStadiums(filters: StadiumFilters) {
  return useQuery({
    queryKey: ['stadiums', filters],
    queryFn: () => api.get<Stadium[]>('/api/v1/stadiums', { params: filters }),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

// ✅ Mutation với optimistic update
function useBookStadium() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateBookingRequest) => 
      api.post('/api/v1/bookings', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bookings'] });
      toast.success('Đặt sân thành công!');
    },
  });
}
```

## Component Architecture

### Atomic Design

```
src/components/
├── ui/               ← shadcn/ui components (đừng sửa)
├── common/           ← Shared components
│   ├── StadiumCard.tsx
│   ├── UserAvatar.tsx
│   └── LoadingSkeleton.tsx
├── forms/            ← Form components
│   ├── BookingForm.tsx
│   └── StadiumSearchForm.tsx
└── layout/           ← Layout components
    ├── Navbar.tsx
    ├── Sidebar.tsx
    └── Footer.tsx
```

### StadiumCard Example (Design Pattern)

```tsx
interface StadiumCardProps {
  stadium: Stadium;
  onBook?: (id: number) => void;
}

export function StadiumCard({ stadium, onBook }: StadiumCardProps) {
  return (
    <div className="group bg-sport-surface border border-white/10 rounded-xl overflow-hidden
                    hover:border-primary-500/40 transition-all duration-200 cursor-pointer">
      {/* Image */}
      <div className="relative h-48 overflow-hidden">
        <Image src={stadium.imageUrl || '/placeholder-stadium.jpg'}
               alt={stadium.stadiumName} fill className="object-cover
               group-hover:scale-105 transition-transform duration-300" />
        <div className="absolute top-3 right-3">
          <StatusBadge status={stadium.stadiumStatus} />
        </div>
      </div>
      {/* Content */}
      <div className="p-4 space-y-3">
        <h3 className="font-semibold text-white truncate">{stadium.stadiumName}</h3>
        <p className="text-sport-muted text-sm truncate">{stadium.address}</p>
        <div className="flex items-center justify-between">
          <span className="text-primary-400 font-semibold">
            {formatVND(stadium.pricePerHour)}/giờ
          </span>
          <StarRating rating={stadium.averageRating} />
        </div>
        <button onClick={() => onBook?.(stadium.stadiumId)}
                className="w-full bg-primary-600 hover:bg-primary-700 text-white 
                           py-2 rounded-lg text-sm font-medium transition-colors">
          Đặt sân
        </button>
      </div>
    </div>
  );
}
```
