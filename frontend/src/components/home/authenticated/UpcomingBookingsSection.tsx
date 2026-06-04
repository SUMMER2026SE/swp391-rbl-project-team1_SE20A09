"use client";

import Link from "next/link";
import { ArrowRight, Calendar, Clock, MapPin } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import type { UpcomingBooking } from "@/lib/home-api";

const STATUS_LABEL: Record<UpcomingBooking["status"], { label: string; className: string }> = {
  confirmed: { label: "Đã xác nhận", className: "bg-green-100 text-green-800" },
  pending: { label: "Chờ xác nhận", className: "bg-amber-100 text-amber-800" },
};

type UpcomingBookingsSectionProps = {
  bookings: UpcomingBooking[];
};

export function UpcomingBookingsSection({ bookings }: UpcomingBookingsSectionProps) {
  return (
    <section className="py-10 md:py-14">
      <div className="container mx-auto px-4">
        <div className="mb-6 flex items-end justify-between gap-4">
          <h2 className="text-2xl font-bold text-foreground">Lịch sân sắp tới</h2>
          <Link
            href="/bookings"
            className="inline-flex items-center text-sm font-medium text-green-800 hover:underline"
          >
            Xem tất cả lịch
            <ArrowRight className="ml-1 h-4 w-4" />
          </Link>
        </div>

        {bookings.length === 0 ? (
          <Card className="border-dashed">
            <CardContent className="flex flex-col items-center py-12 text-center">
              <Calendar className="mb-3 h-12 w-12 text-muted-foreground/50" />
              <p className="text-muted-foreground">
                Bạn chưa có lịch đặt sân nào. Đặt ngay!
              </p>
              <Button className="mt-4 rounded-xl bg-green-800 hover:bg-green-900" asChild>
                <Link href="/booking/new">Đặt sân ngay</Link>
              </Button>
            </CardContent>
          </Card>
        ) : (
          <div className="flex flex-col gap-4">
            {bookings.map((booking) => {
              const status = STATUS_LABEL[booking.status];
              return (
                <Card
                  key={booking.id}
                  className="overflow-hidden transition-shadow hover:shadow-md"
                >
                  <CardContent className="flex flex-col gap-4 p-0 sm:flex-row">
                    <div className="relative h-40 w-full shrink-0 sm:h-auto sm:w-48">
                      <img
                        src={booking.imageUrl}
                        alt={booking.venueName}
                        className="h-full w-full object-cover"
                      />
                    </div>
                    <div className="flex flex-1 flex-col justify-between gap-4 p-4 sm:py-5">
                      <div>
                        <div className="flex flex-wrap items-start justify-between gap-2">
                          <div>
                            <h3 className="text-lg font-semibold">{booking.venueName}</h3>
                            <Badge variant="outline" className="mt-1">
                              {booking.sportType}
                            </Badge>
                          </div>
                          <Badge className={status.className}>{status.label}</Badge>
                        </div>
                        <div className="mt-3 space-y-1.5 text-sm text-muted-foreground">
                          <p className="flex items-center gap-2">
                            <MapPin className="h-4 w-4 shrink-0 text-green-700" />
                            {booking.location}
                          </p>
                          <p className="flex items-center gap-2">
                            <Calendar className="h-4 w-4 shrink-0 text-green-700" />
                            {booking.date}
                          </p>
                          <p className="flex items-center gap-2">
                            <Clock className="h-4 w-4 shrink-0 text-green-700" />
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
                </Card>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
