"use client";

import { cn } from "@/lib/utils";
import { SPORT_CATEGORIES } from "@/lib/home-data";

type SportCategoryChipsProps = {
  active: string;
  onChange: (key: string) => void;
};

export function SportCategoryChips({ active, onChange }: SportCategoryChipsProps) {
  return (
    <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-none [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
      {SPORT_CATEGORIES.map((cat) => (
        <button
          key={cat.key}
          type="button"
          onClick={() => onChange(cat.key)}
          className={cn(
            "inline-flex shrink-0 items-center gap-2 rounded-full border px-4 py-2.5 text-sm font-medium transition-all duration-200",
            active === cat.key
              ? "border-primary bg-primary text-primary-foreground shadow-md shadow-primary/25 scale-[1.02]"
              : "border-border bg-card text-muted-foreground hover:border-primary/40 hover:text-foreground",
          )}
        >
          <span className="text-base" aria-hidden>
            {cat.emoji}
          </span>
          {cat.label}
        </button>
      ))}
    </div>
  );
}
