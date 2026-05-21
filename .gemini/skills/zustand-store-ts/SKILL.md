# SKILL: Zustand State Management (TypeScript)

## Store Setup

```typescript
// src/store/authStore.ts
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

interface User {
  userId: number;
  email: string;
  fullName: string;
  role: 'CUSTOMER' | 'OWNER' | 'ADMIN';
  avatarUrl?: string;
}

interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  // Actions
  setAuth: (user: User, token: string) => void;
  logout: () => void;
  updateUser: (partial: Partial<User>) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      isAuthenticated: false,

      setAuth: (user, accessToken) =>
        set({ user, accessToken, isAuthenticated: true }),

      logout: () =>
        set({ user: null, accessToken: null, isAuthenticated: false }),

      updateUser: (partial) =>
        set((state) => ({
          user: state.user ? { ...state.user, ...partial } : null,
        })),
    }),
    {
      name: 'sportvenue-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        // Chỉ persist những gì cần thiết (không persist actions)
        user: state.user,
        accessToken: state.accessToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
```

## Stadium Search Store (không persist)

```typescript
// src/store/stadiumSearchStore.ts
interface StadiumSearchState {
  sportTypeId: number | null;
  date: Date | null;
  minPrice: number | null;
  maxPrice: number | null;
  searchTerm: string;
  // Actions
  setFilter: (key: keyof Omit<StadiumSearchState, 'setFilter' | 'reset'>,
              value: unknown) => void;
  reset: () => void;
}

const initialState = {
  sportTypeId: null,
  date: null,
  minPrice: null,
  maxPrice: null,
  searchTerm: '',
};

export const useStadiumSearchStore = create<StadiumSearchState>((set) => ({
  ...initialState,
  setFilter: (key, value) => set((state) => ({ ...state, [key]: value })),
  reset: () => set(initialState),
}));
```

## Usage Patterns

```tsx
// ✅ Selector pattern — chỉ subscribe field cần thiết (avoid re-render)
function Navbar() {
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);
  // Component chỉ re-render khi `user` thay đổi
}

// ✅ Computed values
function useIsOwner() {
  return useAuthStore((state) => state.user?.role === 'OWNER');
}

// ✅ Kết hợp với TanStack Query (server state ≠ client state)
function useStadiums() {
  const { sportTypeId, date } = useStadiumSearchStore();

  return useQuery({
    queryKey: ['stadiums', { sportTypeId, date }],
    queryFn: () => api.getStadiums({ sportTypeId, date }),
  });
  // Zustand giữ filter state, TanStack Query giữ fetched data
}
```

## Store Directory Structure

```
src/store/
├── authStore.ts        ← User auth state (persisted)
├── stadiumSearchStore.ts ← Search filters (not persisted)
├── bookingStore.ts     ← Booking form state
└── uiStore.ts          ← UI state (sidebar, modals)
```
