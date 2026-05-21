# SKILL: Tailwind CSS v3 — SportVenue Design

## Project Theme (tailwind.config.ts)

```typescript
// ⚠️ NOTE: tailwind.config.ts cần đổi thành .js nếu gặp lỗi
// (tương tự như next.config.ts → next.config.js)
```

## Dark Sport Theme Classes

```css
/* Backgrounds */
bg-slate-950    /* Page background (sport-dark: #0f172a) */
bg-slate-800    /* Card surface */
bg-slate-700    /* Elevated elements */

/* Green accent */
bg-green-600    /* Primary button */
bg-green-500    /* Hover / highlight */
text-green-400  /* Accent text */
border-green-500/40  /* Border với opacity */

/* Text */
text-white          /* Primary text */
text-slate-300      /* Secondary text */
text-slate-400      /* Muted / placeholder */
text-slate-500      /* Disabled */
```

## Responsive Patterns

```tsx
{/* Mobile-first grid */}
<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">

{/* Stack on mobile, side-by-side on desktop */}
<div className="flex flex-col md:flex-row gap-4">

{/* Hide/show */}
<nav className="hidden md:flex">  {/* Ẩn trên mobile */}
<button className="md:hidden">    {/* Chỉ hiện trên mobile */}
```

## Component Patterns

```tsx
{/* Card với glass effect */}
<div className="bg-slate-800/60 backdrop-blur-sm border border-white/10 
                rounded-xl p-6 hover:border-green-500/30 transition-all duration-200">

{/* Input với dark theme */}
<input className="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-2.5
                  text-white placeholder-slate-500 focus:outline-none 
                  focus:ring-2 focus:ring-green-500 focus:border-transparent
                  transition-all duration-200" />

{/* Badge status */}
<span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium
                 bg-green-500/15 text-green-400 border border-green-500/25">
  <span className="w-1.5 h-1.5 rounded-full bg-green-400" />
  Available
</span>

{/* Primary button với shimmer effect */}
<button className="relative overflow-hidden bg-green-600 hover:bg-green-500 
                   text-white font-medium px-6 py-2.5 rounded-lg
                   transition-all duration-200 active:scale-95
                   before:absolute before:inset-0 before:bg-white/10 
                   before:translate-x-[-100%] hover:before:translate-x-[100%]
                   before:transition-transform before:duration-500">
  Đặt sân ngay
</button>
```

## Spacing Scale (use consistently)

```
p-1   → 4px    (icon padding)
p-2   → 8px    (small elements)
p-4   → 16px   (card content padding)
p-6   → 24px   (section padding)
p-8   → 32px   (large sections)
p-16  → 64px   (hero sections)

gap-2 → 8px    (small gaps)
gap-4 → 16px   (standard gap)
gap-6 → 24px   (card gap)
gap-8 → 32px   (section gap)
```

## Animation with Tailwind

```tsx
{/* Fade in on mount */}
<div className="animate-fade-in">  {/* Cần define trong config */}

{/* Pulse loading */}
<div className="animate-pulse bg-slate-700 rounded-lg h-48 w-full" />

{/* Spin loader */}
<div className="animate-spin rounded-full h-8 w-8 border-2 border-slate-700 border-t-green-500" />

{/* Bounce notification dot */}
<span className="absolute -top-1 -right-1 w-2 h-2 bg-red-500 rounded-full animate-bounce" />
```
