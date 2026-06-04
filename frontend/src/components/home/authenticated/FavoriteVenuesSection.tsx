"use client";

import Link from "next/link";
import { Heart } from "lucide-react";
import { Button } from "@/components/ui/button";
import { VenueCard } from "@/components/landing/VenueCard";
import type { FeaturedVenue } from "@/lib/home-data";

type FavoriteVenuesSectionProps = {
  venues: FeaturedVenue[];
};

export function FavoriteVenuesSection({ venues }: FavoriteVenuesSectionProps) {
  return (
    <section className="border-t bg-muted/30 py-10 md:py-14">
      <div className="container mx-auto px-4">
        <h2 className="mb-6 text-2xl font-bold">Sân yêu thích của bạn</h2>

        {venues.length === 0 ? (
          <div className="rounded-2xl border border-dashed bg-card py-12 text-center">
            <Heart className="mx-auto mb-3 h-10 w-10 text-muted-foreground/40" />
            <p className="text-muted-foreground">
              Chưa có sân yêu thích. Khám phá sân ngay!
            </p>
            <Button className="mt-4 rounded-xl bg-green-800 hover:bg-green-900" asChild>
              <Link href="/search">Khám phá sân</Link>
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {venues.map((venue) => (
              <VenueCard
                key={venue.id}
                {...venue}
                saved
                actionLabel="Đặt lại"
                actionHref={`/booking/new?venue=${venue.id}`}
              />
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
