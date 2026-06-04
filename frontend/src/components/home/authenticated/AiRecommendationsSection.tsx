"use client";

import { useMemo, useState } from "react";
import { Sparkles } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { VenueCard } from "@/components/landing/VenueCard";
import { cn } from "@/lib/utils";
import {
  RECOMMENDATION_FILTERS,
  type RecommendationFilter,
} from "@/lib/authenticated-home-data";
import type { FeaturedVenue } from "@/lib/home-data";

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
    <section className="py-10 md:py-14">
      <div className="container mx-auto px-4">
        <div className="mb-2 flex flex-wrap items-center gap-2">
          <h2 className="text-2xl font-bold">Gợi ý dành cho bạn</h2>
          <Badge className="gap-1 border-0 bg-violet-100 text-violet-800">
            <Sparkles className="h-3 w-3" />
            AI
          </Badge>
        </div>
        <p className="mb-6 max-w-2xl text-muted-foreground">
          Dựa trên lịch sử đặt sân và môn thể thao bạn yêu thích
        </p>

        <div className="mb-8 flex flex-wrap gap-2">
          {RECOMMENDATION_FILTERS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => setFilter(tab.key)}
              className={cn(
                "rounded-full border px-4 py-2 text-sm font-medium transition-all",
                filter === tab.key
                  ? "border-green-800 bg-green-800 text-white shadow-md"
                  : "border-border bg-card text-muted-foreground hover:border-green-800/40",
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((venue) => (
            <VenueCard key={venue.id} {...venue} />
          ))}
        </div>
      </div>
    </section>
  );
}
