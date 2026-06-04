"use client";

import { useMemo, useState } from "react";
import { Sparkles } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { VenueCard } from "@/components/landing/VenueCard";
import { SectionHeading } from "@/components/home/authenticated/decor/SectionHeading";
import { cn } from "@/lib/utils";
import {
  RECOMMENDATION_FILTERS,
  type RecommendationFilter,
} from "@/lib/authenticated-home-data";
import type { FeaturedVenue } from "@/lib/home-data";

const STAGGER = [
  "animation-delay-500",
  "animation-delay-700",
  "animation-delay-900",
  "",
  "animation-delay-300",
  "animation-delay-1100",
];

type AiRecommendationsSectionProps = {
  venues: FeaturedVenue[];
};

export function AiRecommendationsSection({ venues }: AiRecommendationsSectionProps) {
  const [filter, setFilter] = useState<RecommendationFilter>("nearby");

  const filtered = useMemo(() => {
    const list = [...venues];
    if (filter === "price") {
      return list.sort((a, b) => a.price - b.price);
    }
    if (filter === "rating") {
      return list.sort((a, b) => b.rating - a.rating);
    }
    return list;
  }, [venues, filter]);

  return (
    <section className="relative py-10 md:py-14">
      <div className="container mx-auto px-4">
        <SectionHeading
          title="Gợi ý dành cho bạn"
          subtitle="Dựa trên lịch sử đặt sân và môn thể thao bạn yêu thích"
          badge={
            <Badge className="animate-glow-pulse gap-1 border-0 bg-gradient-to-r from-violet-500 to-fuchsia-500 px-3 py-1 text-white shadow-lg shadow-violet-500/30">
              <Sparkles className="h-3.5 w-3.5" />
              AI
            </Badge>
          }
        />

        <div className="mb-8 flex flex-wrap gap-2 animate-fade-in-up animation-delay-300">
          {RECOMMENDATION_FILTERS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => setFilter(tab.key)}
              className={cn(
                "rounded-full border px-5 py-2.5 text-sm font-medium transition-all duration-300",
                filter === tab.key
                  ? "border-transparent bg-gradient-to-r from-green-800 to-emerald-600 text-white shadow-lg shadow-emerald-800/30 scale-105"
                  : "border-border/80 bg-white/70 text-muted-foreground backdrop-blur-sm hover:border-emerald-400/50 hover:text-green-900 hover:shadow-md",
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((venue, index) => (
            <div
              key={venue.id}
              className={cn(
                "animate-fade-in-up transition-transform duration-300 hover:-translate-y-1",
                STAGGER[index % STAGGER.length],
              )}
            >
              <VenueCard {...venue} />
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
