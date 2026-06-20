"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { ArrowRight, Flame } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { VenueCard } from "@/components/landing/VenueCard";
import { SportCategoryChips } from "@/components/landing/home/SportCategoryChips";
import { searchStadiums } from "@/lib/api/stadium";

export function FeaturedVenuesSection() {
  const [sportFilter, setSportFilter] = useState("all");

  const { data, isLoading } = useQuery({
    queryKey: ["featured-venues"],
    queryFn: () => searchStadiums({ size: 6, page: 0 }),
    staleTime: 60_000,
  });

  const filtered = useMemo(() => {
    const venues = data?.content ?? [];
    if (sportFilter === "all") return venues;
    // API trả về sportName tiếng Anh ("Football"), map sang key để so sánh
    const sportKeyMap: Record<string, string> = {
      Football: "football",
      Badminton: "badminton",
      Basketball: "basketball",
      Tennis: "tennis",
      Pickleball: "pickleball",
    };
    return venues.filter((v) => sportKeyMap[v.sportName] === sportFilter);
  }, [sportFilter, data?.content]);

  return (
    <section id="featured-venues" className="py-20 md:py-28">
      <div className="container mx-auto px-4">
        <div className="mb-10 flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
          <div className="max-w-xl">
            <div className="mb-3 inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-primary">
              <Flame className="h-3.5 w-3.5" />
              Sân nổi bật
            </div>
            <h2 className="text-3xl font-bold tracking-tight md:text-4xl">
              Được yêu thích nhất tuần này
            </h2>
            <p className="mt-3 text-muted-foreground text-lg">
              Danh sách sân chất lượng cao, đánh giá tốt và còn nhiều khung giờ trống.
            </p>
          </div>
          <Button variant="outline" className="shrink-0 rounded-full" asChild>
            <Link href="/search">
              Xem tất cả sân
              <ArrowRight className="ml-2 h-4 w-4" />
            </Link>
          </Button>
        </div>

        <SportCategoryChips active={sportFilter} onChange={setSportFilter} />

        {isLoading ? (
          <div className="mt-10 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="h-80 animate-pulse rounded-2xl bg-muted" />
            ))}
          </div>
        ) : filtered.length > 0 ? (
          <div className="mt-10 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {filtered.map((venue, index) => (
              <div
                key={venue.stadiumId}
                className="animate-fade-in-up"
                style={{ animationDelay: `${index * 80}ms` }}
              >
                <VenueCard
                  id={venue.stadiumId}
                  image={venue.firstImageUrl ?? ""}
                  name={venue.stadiumName}
                  sportType={venue.sportName}
                  price={venue.pricePerHour}
                  rating={venue.averageRating ?? 5}
                  location={venue.address}
                />
              </div>
            ))}
          </div>
        ) : (
          <div className="mt-16 rounded-2xl border border-dashed bg-muted/30 py-16 text-center">
            <p className="text-muted-foreground">
              Chưa có sân cho môn này. Thử chọn môn khác hoặc{" "}
              <Link href="/search" className="font-medium text-primary hover:underline">
                tìm kiếm đầy đủ
              </Link>
              .
            </p>
          </div>
        )}
      </div>
    </section>
  );
}
