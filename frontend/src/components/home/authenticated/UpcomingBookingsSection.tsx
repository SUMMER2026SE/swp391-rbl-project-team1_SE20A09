"use client";

import Link from "next/link";
import { ArrowRight, Calendar, Clock, MapPin } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { CardContent } from "@/components/ui/card";
import { SectionHeading } from "@/components/home/authenticated/decor/SectionHeading";
import { GlowCard } from "@/components/home/authenticated/decor/GlowCard";
import { cn } from "@/lib/utils";
import type { UpcomingBooking } from "@/lib/home-api";
import Image from "next/image";

const STATUS_LABEL: Record<UpcomingBooking["status"], { label: string; className: string }> = {
  confirmed: { label: "Đã xác nhận", className: "bg-green-100 text-green-800" },
  pending: { label: "Chờ xác nhận", className: "bg-amber-100 text-amber-800" },
};

const STAGGER = ["", "animation-delay-300", "animation-delay-500", "animation-delay-700"];

type UpcomingBookingsSectionProps = {
  bookings: UpcomingBooking[];
};

export function UpcomingBookingsSection({ bookings }: UpcomingBookingsSectionProps) {
  return (
    <section className="relative py-10 md:py-14">
      <div className="container mx-auto px-4">
        <SectionHeading
          title="Lịch sân sắp tới"
          subtitle="Theo dõi các buổi chơi sắp diễn ra — đừng bỏ lỡ nhé"
          action={
            <Link
              href="/bookings"
              className="inline-flex items-center rounded-full border border-emerald-200 bg-white/80 px-4 py-2 text-sm font-medium text-green-800 shadow-sm backdrop-blur-sm transition-all hover:border-green-400 hover:shadow-md"
            >
              Xem tất cả lịch
              <ArrowRight className="ml-1.5 h-4 w-4" />
            </Link>
          }
        />

        {bookings.length === 0 ? (
          <GlowCard className="animation-delay-300">
            <CardContent className="flex flex-col items-center border border-dashed border-emerald-200/60 py-14 text-center">
              <Calendar className="mb-3 h-14 w-14 text-emerald-300" />
              <p className="text-muted-foreground">Bạn chưa có lịch đặt sân nào. Đặt ngay!</p>
              <Button
                className="home-cta-shine mt-5 rounded-xl bg-green-800 hover:bg-green-900"
                asChild
              >
                <Link href="/booking/new">Đặt sân ngay</Link>
              </Button>
            </CardContent>
          </GlowCard>
        ) : (
          <div className="flex flex-col gap-5">
            {bookings.map((booking, index) => {
              const status = STATUS_LABEL[booking.status];
              return (
                <GlowCard
                  key={booking.id}
                  delayClass={STAGGER[index % STAGGER.length]}
                  className="transition-transform duration-300 hover:-translate-y-0.5"
                >
                  <CardContent className="flex flex-col gap-4 p-0 sm:flex-row">
                    <div className="relative h-44 w-full shrink-0 overflow-hidden sm:h-auto sm:w-52">
                      <Image
                        src={booking.imageUrl}
                        alt={booking.venueName}
                        fill
                        className="object-cover transition-transform duration-500 group-hover:scale-105"
                        unoptimized
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent" />
                    </div>
                    <div className="flex flex-1 flex-col justify-between gap-4 p-5">
                      <div>
                        <div className="flex flex-wrap items-start justify-between gap-2">
                          <div>
                            <h3 className="text-lg font-semibold text-foreground">
                              {booking.venueName}
                            </h3>
                            <Badge variant="outline" className="mt-1 border-emerald-200">
                              {booking.sportType}
                            </Badge>
                          </div>
                          <Badge className={cn(status.className, "shadow-sm")}>
                            {status.label}
                          </Badge>
                        </div>
                        <div className="mt-3 space-y-2 text-sm text-muted-foreground">
                          <p className="flex items-center gap-2">
                            <MapPin className="h-4 w-4 shrink-0 text-emerald-600" />
                            {booking.location}
                          </p>
                          <p className="flex items-center gap-2">
                            <Calendar className="h-4 w-4 shrink-0 text-emerald-600" />
                            {booking.date}
                          </p>
                          <p className="flex items-center gap-2">
                            <Clock className="h-4 w-4 shrink-0 text-emerald-600" />
                            {booking.time}
                          </p>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <Button variant="outline" size="sm" className="rounded-lg" asChild>
                          <Link href={`/booking/${booking.id}`}>Xem chi tiết</Link>
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          className="rounded-lg text-destructive hover:bg-destructive/10 hover:text-destructive"
                        >
                          Hủy lịch
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </GlowCard>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
