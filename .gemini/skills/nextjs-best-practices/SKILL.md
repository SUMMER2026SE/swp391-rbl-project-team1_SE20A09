# SKILL: Next.js 14 Best Practices

## Project-specific Notes

- Dùng `next.config.js` (**không** `.ts`) — Next.js 14 không support TypeScript config
- Output: `standalone` — tối ưu cho Docker
- API proxy: `/api/*` → `http://localhost:8080/api/*`

## App Router File Conventions

```
app/
├── layout.tsx          ← Root layout (không thêm 'use client')
├── page.tsx            ← Home page (Server Component)
├── loading.tsx         ← Loading UI (Suspense boundary)
├── error.tsx           ← Error UI ('use client' required)
├── not-found.tsx       ← 404 page
└── [slug]/
    ├── page.tsx        ← Dynamic route
    └── @modal/         ← Parallel routes (advanced)
```

## Data Fetching

```tsx
// Server Component — async/await trực tiếp
async function StadiumsPage() {
  const stadiums = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/v1/stadiums`, {
    next: { revalidate: 60 } // ISR — revalidate mỗi 60s
  }).then(r => r.json());
  return <StadiumList stadiums={stadiums} />;
}

// Client Component — TanStack Query
'use client';
function BookingHistory() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['bookings', 'my'],
    queryFn: () => api.get('/api/v1/bookings/my').then(r => r.data),
  });
  if (isLoading) return <Skeleton />;
  if (error) return <ErrorMessage />;
  return <BookingList bookings={data} />;
}
```

## Metadata & SEO

```tsx
// Static metadata
export const metadata: Metadata = {
  title: 'SportVenue — Đặt sân thể thao online',
  description: 'Tìm kiếm và đặt sân thể thao tiện lợi, nhanh chóng',
};

// Dynamic metadata
export async function generateMetadata({ params }: { params: { id: string } }) {
  const stadium = await fetchStadium(params.id);
  return {
    title: `${stadium.name} — SportVenue`,
    description: stadium.description,
    openGraph: { images: [stadium.imageUrl] },
  };
}
```

## Environment Variables

```
NEXT_PUBLIC_*   ← Exposed to browser (public)
Không có prefix ← Server-only (secret)

# next.config.js — fallback nếu env không set
destination: `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/:path*`
```

## Performance

```tsx
// ✅ Dynamic import cho heavy components
const Map = dynamic(() => import('@/components/StadiumMap'), {
  loading: () => <Skeleton className="h-64" />,
  ssr: false  // Map thường không cần SSR
});

// ✅ Image optimization
import Image from 'next/image';
<Image src={imageUrl} alt={name} width={400} height={300}
       className="object-cover" priority={isAboveFold} />

// ✅ Font optimization
import { Inter } from 'next/font/google';
const inter = Inter({ subsets: ['latin'], variable: '--font-inter' });
```
