# SKILL: React Patterns — SportVenue Frontend

## Custom Hooks

### useDebounce — Search optimization

```typescript
// src/hooks/useDebounce.ts
export function useDebounce<T>(value: T, delay: number = 300): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debouncedValue;
}

// Usage in stadium search
function StadiumSearch() {
  const [term, setTerm] = useState('');
  const debouncedTerm = useDebounce(term, 400);

  const { data } = useQuery({
    queryKey: ['stadiums', debouncedTerm],
    queryFn: () => api.searchStadiums(debouncedTerm),
  });
}
```

### usePagination

```typescript
// src/hooks/usePagination.ts
export function usePagination(totalPages: number, initialPage = 0) {
  const [page, setPage] = useState(initialPage);

  return {
    page,
    isFirst: page === 0,
    isLast: page >= totalPages - 1,
    goTo: (p: number) => setPage(Math.max(0, Math.min(p, totalPages - 1))),
    next: () => setPage(p => Math.min(p + 1, totalPages - 1)),
    prev: () => setPage(p => Math.max(p - 1, 0)),
  };
}
```

### useAuth

```typescript
// src/hooks/useAuth.ts
export function useAuth() {
  const { user, accessToken, isAuthenticated, logout } = useAuthStore();

  const hasRole = (role: User['role']) => user?.role === role;

  return {
    user,
    accessToken,
    isAuthenticated,
    isCustomer: hasRole('CUSTOMER'),
    isOwner: hasRole('OWNER'),
    isAdmin: hasRole('ADMIN'),
    logout,
  };
}
```

## Component Patterns

### Compound Components (Tabs, Accordion)

```tsx
// Usage
<Tabs defaultValue="info">
  <Tabs.List>
    <Tabs.Tab value="info">Thông tin</Tabs.Tab>
    <Tabs.Tab value="reviews">Đánh giá</Tabs.Tab>
  </Tabs.List>
  <Tabs.Panel value="info"><StadiumInfo /></Tabs.Panel>
  <Tabs.Panel value="reviews"><Reviews /></Tabs.Panel>
</Tabs>
```

### Loading States Pattern

```tsx
// ✅ Skeleton loading (UX tốt hơn spinner)
function StadiumCardSkeleton() {
  return (
    <div className="bg-slate-800 rounded-xl overflow-hidden animate-pulse">
      <div className="h-48 bg-slate-700" />
      <div className="p-4 space-y-3">
        <div className="h-5 bg-slate-700 rounded w-3/4" />
        <div className="h-4 bg-slate-700 rounded w-1/2" />
        <div className="h-9 bg-slate-700 rounded" />
      </div>
    </div>
  );
}

// Usage
{isLoading
  ? Array.from({ length: 6 }).map((_, i) => <StadiumCardSkeleton key={i} />)
  : stadiums.map(s => <StadiumCard key={s.stadiumId} stadium={s} />)
}
```

### Error Boundary

```tsx
'use client';
// app/error.tsx — Next.js error boundary
export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
      <h2 className="text-xl font-semibold text-white">Có lỗi xảy ra</h2>
      <p className="text-slate-400">{error.message || 'Vui lòng thử lại'}</p>
      <button onClick={reset}
              className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg">
        Thử lại
      </button>
    </div>
  );
}
```

## Performance Patterns

```tsx
// ✅ memo cho expensive re-renders
const StadiumCard = memo(({ stadium, onBook }: StadiumCardProps) => {
  // ...
});

// ✅ useCallback cho event handlers trong list
const handleBook = useCallback((id: number) => {
  router.push(`/stadiums/${id}/book`);
}, [router]);

// ✅ useMemo cho expensive computations
const sortedStadiums = useMemo(() =>
  [...stadiums].sort((a, b) => b.averageRating - a.averageRating),
  [stadiums]
);
```
