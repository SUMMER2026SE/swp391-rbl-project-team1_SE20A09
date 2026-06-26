"use client";

import Link from "next/link";
import { History } from "lucide-react";
import { Button } from "@/components/ui/button";
import { VenueCard } from "@/components/landing/VenueCard";
import { SectionHeading } from "@/components/home/authenticated/decor/SectionHeading";
import { GlowCard } from "@/components/home/authenticated/decor/GlowCard";
import { cn } from "@/lib/utils";
import type { FeaturedVenue } from "@/lib/home-data";

const STAGGER = [
  "animation-delay-300",
  "animation-delay-500",
  "animation-delay-700",
  "animation-delay-900",
  "animation-delay-1100",
  "",
];

type FavoriteVenuesSectionProps = {
  venues: FeaturedVenue[];
};

export function FavoriteVenuesSection({ venues }: FavoriteVenuesSectionProps) {
  return (
    <section className="relative border-t border-emerald-100/80 py-10 md:py-14">
      <div className="absolute inset-0 bg-gradient-to-b from-muted/40 via-emerald-50/30 to-transparent" />
      <div className="container relative mx-auto px-4">
        <SectionHeading
          title="Sân đã chơi gần đây"
          subtitle="Đặt lại nhanh các sân bạn đã chơi — một chạm là xong"
          delayClass="animation-delay-300"
          badge={
            <History className="h-5 w-5 text-emerald-600 animate-float-gentle" />
          }
        />

        {venues.length === 0 ? (
          <GlowCard className="animation-delay-300">
            <div className="flex flex-col items-center py-14 text-center">
              <History className="mb-3 h-12 w-12 text-emerald-300" />
              <p className="text-muted-foreground">Bạn chưa đặt sân nào gần đây. Khám phá sân và trải nghiệm ngay!</p>
              <Button
                className="home-cta-shine mt-5 rounded-xl bg-green-800 hover:bg-green-900"
                asChild
              >
                <Link href="/venues">Khám phá sân</Link>
              </Button>
            </div>
          </GlowCard>
        ) : (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {venues.map((venue, index) => (
              <div
                key={venue.id}
                className={cn("animate-fade-in-up", STAGGER[index % STAGGER.length])}
              >
                <VenueCard
                  {...venue}
                  actionLabel="Đặt lại"
                  actionHref={`/booking/new?venue=${venue.id}`}
                />
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

