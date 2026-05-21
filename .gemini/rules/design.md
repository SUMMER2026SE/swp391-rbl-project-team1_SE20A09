# Design System — Sport Venue UI/UX

## Design Philosophy

**"Dynamic, Premium, Dark Sport"** — Giao diện phải truyền cảm giác năng động, chuyên nghiệp như các platform thể thao quốc tế (Nike, Adidas apps).

## Color Palette

```css
/* Primary — Sport Green */
--primary-500: #22c55e;   /* Main action color */
--primary-600: #16a34a;   /* Hover state */
--primary-700: #15803d;   /* Active/pressed */

/* Dark Background */
--sport-dark:    #0f172a;  /* Page background */
--sport-surface: #1e293b;  /* Cards, panels */
--sport-muted:   #475569;  /* Placeholder text */

/* Text */
--foreground:    #f8fafc;  /* Primary text */
--muted-fg:      #94a3b8;  /* Secondary text */
```

## Typography

```css
/* Font: Inter (Google Fonts) */
font-family: 'Inter', system-ui, sans-serif;

/* Scale */
.text-xs    /* 12px — labels, badges */
.text-sm    /* 14px — body small */
.text-base  /* 16px — body default */
.text-lg    /* 18px — card titles */
.text-xl    /* 20px — section headers */
.text-2xl   /* 24px — page titles */
.text-4xl   /* 36px — hero headlines */
```

## Component Patterns

### Cards

```tsx
// ✅ Standard card pattern
<div className="bg-sport-surface border border-white/10 rounded-xl p-6 
                hover:border-primary-500/50 transition-all duration-200">
  {/* content */}
</div>
```

### Buttons

```tsx
// Primary — green, filled
<button className="bg-primary-600 hover:bg-primary-700 text-white 
                   px-4 py-2 rounded-lg font-medium transition-colors">

// Secondary — ghost
<button className="border border-white/20 hover:border-primary-500/50 
                   text-white px-4 py-2 rounded-lg transition-colors">

// Danger
<button className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg">
```

### Status Badges

```tsx
const statusColor = {
  Available:   'bg-green-500/20 text-green-400 border-green-500/30',
  Maintenance: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
  Closed:      'bg-red-500/20 text-red-400 border-red-500/30',
};
<span className={`px-2 py-1 rounded-full text-xs border ${statusColor[status]}`}>
  {status}
</span>
```

## Animation Guidelines

```css
/* ✅ Subtle transitions — không quá flashy */
transition-all duration-200   /* hover effects */
transition-all duration-300   /* panel open/close */

/* ✅ Entrance animations */
animate-fade-in               /* page content */
animate-slide-up              /* modals, drawers */

/* ❌ Tránh animations > 500ms — cảm giác chậm */
/* ❌ Tránh rotation/bounce trừ loading spinner */
```

## Layout

- **Max width:** `max-w-7xl mx-auto px-4 sm:px-6 lg:px-8`
- **Grid:** `grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6`
- **Sidebar:** 280px fixed, content fluid
- **Mobile-first:** Design cho 375px trước, scale lên desktop

## shadcn/ui Usage

Ưu tiên dùng shadcn components thay vì tự build:
- Form inputs → `<Input>`, `<Select>`, `<Textarea>`
- Dialogs → `<Dialog>`, `<AlertDialog>`
- Navigation → `<NavigationMenu>`, `<Breadcrumb>`
- Data display → `<Table>`, `<Badge>`, `<Avatar>`
- Feedback → `<Toast>`, `<Alert>`, `<Skeleton>`
