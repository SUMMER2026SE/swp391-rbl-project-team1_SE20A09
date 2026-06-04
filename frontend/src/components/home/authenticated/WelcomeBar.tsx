"use client";

import Link from "next/link";
import { CalendarCheck, Heart, Sparkles, Zap } from "lucide-react";
import { Button } from "@/components/ui/button";

type WelcomeBarProps = {
  displayName: string;
  bookingCount: number;
  favoriteCount: number;
  rewardPoints: number;
};

export function WelcomeBar({
  displayName,
  bookingCount,
  favoriteCount,
  rewardPoints,
}: WelcomeBarProps) {
  return (
    <section className="border-b border-green-100 bg-[#f0fdf4]">
      <div className="container mx-auto flex flex-col gap-6 px-4 py-8 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-green-900 md:text-3xl">
            Chào mừng trở lại, {displayName}! 👋
          </h1>
          <p className="mt-1 text-green-800/80">
            Sẵn sàng cho buổi tập hoặc trận đấu tiếp theo?
          </p>
          <div className="mt-4 flex flex-wrap gap-4 text-sm">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-white/80 px-3 py-1.5 font-medium text-green-900 shadow-sm">
              <CalendarCheck className="h-4 w-4 text-green-700" />
              {bookingCount} lượt đặt sân
            </span>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-white/80 px-3 py-1.5 font-medium text-green-900 shadow-sm">
              <Heart className="h-4 w-4 text-rose-500" />
              {favoriteCount} sân yêu thích
            </span>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-white/80 px-3 py-1.5 font-medium text-green-900 shadow-sm">
              <Sparkles className="h-4 w-4 text-amber-500" />
              {rewardPoints.toLocaleString("vi-VN")} điểm thưởng
            </span>
          </div>
        </div>
        <Button
          size="lg"
          className="h-12 shrink-0 rounded-xl bg-green-800 px-8 font-semibold shadow-lg shadow-green-800/20 hover:bg-green-900"
          asChild
        >
          <Link href="/booking/new">
            <Zap className="mr-2 h-5 w-5" />
            Đặt sân nhanh
          </Link>
        </Button>
      </div>
    </section>
  );
}
