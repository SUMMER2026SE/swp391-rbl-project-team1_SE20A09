"use client";

import Link from "next/link";
import { MapPin, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { CardContent } from "@/components/ui/card";
import { SectionHeading } from "@/components/home/authenticated/decor/SectionHeading";
import { GlowCard } from "@/components/home/authenticated/decor/GlowCard";
import type { CommunityEvent } from "@/lib/home-api";

const STAGGER = ["animation-delay-300", "animation-delay-500"];

type CommunityFeedSectionProps = {
  events: CommunityEvent[];
};

export function CommunityFeedSection({ events }: CommunityFeedSectionProps) {
  return (
    <section className="relative border-t border-emerald-100/80 py-10 md:py-14">
      <div className="absolute inset-0 bg-gradient-to-b from-violet-50/20 via-muted/30 to-transparent" />
      <div className="container relative mx-auto px-4">
        <SectionHeading
          title="Hoạt động cộng đồng"
          subtitle="Tham gia trận giao hữu — kết nối với cộng đồng thể thao"
          action={
            <Link
              href="/community"
              className="text-sm font-medium text-green-800 transition-colors hover:text-emerald-600 hover:underline"
            >
              Xem cộng đồng →
            </Link>
          }
        />

        <div className="grid gap-5 md:grid-cols-2">
          {events.map((event, index) => (
            <GlowCard
              key={event.id}
              delayClass={STAGGER[index % STAGGER.length]}
              className="transition-transform hover:-translate-y-0.5"
            >
              <CardContent className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h3 className="text-lg font-semibold">{event.title}</h3>
                  <Badge variant="outline" className="mt-2 border-violet-200">
                    {event.sportType}
                  </Badge>
                  <p className="mt-2 text-sm text-muted-foreground">{event.datetime}</p>
                  <p className="mt-1 flex items-center gap-1 text-sm text-muted-foreground">
                    <MapPin className="h-3.5 w-3.5 text-emerald-600" />
                    {event.location}
                  </p>
                  <p className="mt-2 flex items-center gap-1 text-sm font-semibold text-amber-700">
                    <Users className="h-4 w-4" />
                    Còn thiếu {event.slotsNeeded} người
                  </p>
                </div>
                <Button
                  className="home-cta-shine shrink-0 rounded-xl bg-green-800 hover:bg-green-900"
                  asChild
                >
                  <Link href="/community">Tham gia</Link>
                </Button>
              </CardContent>
            </GlowCard>
          ))}
        </div>
      </div>
    </section>
  );
}
