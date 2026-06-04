"use client";

import Link from "next/link";
import { MapPin, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import type { CommunityEvent } from "@/lib/home-api";

type CommunityFeedSectionProps = {
  events: CommunityEvent[];
};

export function CommunityFeedSection({ events }: CommunityFeedSectionProps) {
  return (
    <section className="border-t bg-muted/30 py-10 md:py-14">
      <div className="container mx-auto px-4">
        <div className="mb-6 flex items-end justify-between gap-4">
          <h2 className="text-2xl font-bold">Hoạt động cộng đồng</h2>
          <Link
            href="/community"
            className="text-sm font-medium text-green-800 hover:underline"
          >
            Xem cộng đồng
          </Link>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          {events.map((event) => (
            <Card key={event.id} className="transition-shadow hover:shadow-md">
              <CardContent className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h3 className="font-semibold">{event.title}</h3>
                  <Badge variant="outline" className="mt-2">
                    {event.sportType}
                  </Badge>
                  <p className="mt-2 text-sm text-muted-foreground">{event.datetime}</p>
                  <p className="mt-1 flex items-center gap-1 text-sm text-muted-foreground">
                    <MapPin className="h-3.5 w-3.5 text-green-700" />
                    {event.location}
                  </p>
                  <p className="mt-2 flex items-center gap-1 text-sm font-medium text-amber-700">
                    <Users className="h-4 w-4" />
                    Còn thiếu {event.slotsNeeded} người
                  </p>
                </div>
                <Button
                  className="shrink-0 rounded-xl bg-green-800 hover:bg-green-900"
                  asChild
                >
                  <Link href="/community">Tham gia</Link>
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </section>
  );
}
