"use client";

import Link from "next/link";
import { Zap } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SparkleField } from "@/components/home/authenticated/decor/SparkleField";
import { cn } from "@/lib/utils";

type WelcomeBarProps = {
  displayName: string;
  bookingCount: number;
  favoriteCount: number;
  rewardPoints: number;
};

const FLOATING_ICONS = [
  { emoji: "⚽", className: "left-[6%] top-[18%] text-3xl animation-delay-300", delay: "" },
  { emoji: "🏸", className: "right-[12%] top-[22%] text-2xl animation-delay-700", delay: "" },
  { emoji: "🎾", className: "left-[18%] bottom-[20%] text-2xl animation-delay-500 opacity-70", delay: "" },
  { emoji: "🏀", className: "right-[8%] bottom-[28%] text-3xl animation-delay-900 opacity-80", delay: "" },
] as const;

const STAT_PILLS = [
  {
    key: "bookings",
    emoji: "📅",
    bg: "from-sky-100 to-blue-50",
    label: (n: number) => `${n} lượt đặt sân`,
    delay: "animation-delay-300",
  },
  {
    key: "favorites",
    emoji: "💚",
    bg: "from-rose-100 to-pink-50",
    label: (n: number) => `${n} sân yêu thích`,
    delay: "animation-delay-500",
  },
  {
    key: "points",
    emoji: "✨",
    bg: "from-amber-100 to-yellow-50",
    label: (n: number) => `${n.toLocaleString("vi-VN")} điểm thưởng`,
    delay: "animation-delay-700",
  },
] as const;

export function WelcomeBar({
  displayName,
  bookingCount,
  favoriteCount,
  rewardPoints,
}: WelcomeBarProps) {
  const counts = [bookingCount, favoriteCount, rewardPoints];

  return (
    <section className="relative overflow-hidden border-b border-emerald-200/60">
      <div className="absolute inset-0 bg-gradient-to-br from-emerald-100/80 via-[#ecfdf5] to-teal-50/90" />
      <SparkleField />

      {FLOATING_ICONS.map((item) => (
        <span
          key={item.emoji}
          className={cn(
            "pointer-events-none absolute select-none opacity-40 drop-shadow-sm",
            "animate-float-drift",
            item.className,
          )}
          aria-hidden
        >
          {item.emoji}
        </span>
      ))}

      <div className="container relative mx-auto flex flex-col gap-6 px-4 py-10 md:flex-row md:items-center md:justify-between md:py-12">
        <div>
          <h1 className="flex flex-wrap items-baseline gap-x-2 gap-y-1 text-3xl font-bold leading-tight md:text-4xl">
            <span className="animate-fade-in-up text-shimmer">Chào mừng trở lại,</span>
            <span className="animate-fade-in-up animation-delay-300 inline-flex items-baseline gap-2">
              <span className="animate-float-gentle bg-gradient-to-r from-green-900 via-emerald-700 to-green-900 bg-clip-text font-bold text-transparent">
                {displayName}
              </span>
              <span className="animate-wiggle" aria-hidden>
                👋
              </span>
            </span>
          </h1>
          <p className="mt-3 max-w-lg animate-fade-in-up animation-delay-500 text-base text-green-900/75">
            <span className="mr-1.5 inline-block animate-float-gentle" aria-hidden>
              🏃
            </span>
            Sẵn sàng cho buổi tập hoặc trận đấu tiếp theo? Hệ thống đã gợi ý sân phù hợp cho bạn.
          </p>
          <div className="mt-5 flex flex-wrap gap-3">
            {STAT_PILLS.map((pill, i) => (
              <span
                key={pill.key}
                className={cn(
                  "inline-flex items-center gap-2.5 rounded-2xl border border-white/90 px-4 py-2.5 text-sm font-semibold text-green-900 shadow-md shadow-emerald-900/5 backdrop-blur-md",
                  "bg-gradient-to-br transition-all duration-300 hover:scale-105 hover:shadow-lg",
                  "animate-fade-in-up",
                  pill.bg,
                  pill.delay,
                )}
              >
                <span
                  className="flex h-9 w-9 items-center justify-center rounded-xl bg-white/80 text-lg shadow-inner animate-float-gentle"
                  aria-hidden
                >
                  {pill.emoji}
                </span>
                {pill.label(counts[i])}
              </span>
            ))}
          </div>
        </div>
        <Button
          size="lg"
          className={cn(
            "home-cta-shine h-14 shrink-0 rounded-2xl border border-emerald-600/30 bg-gradient-to-r from-green-800 via-emerald-700 to-green-800 px-10 text-base font-bold shadow-xl shadow-emerald-900/25 transition-all hover:scale-[1.03] hover:from-green-900 hover:via-emerald-800 hover:to-green-900 hover:shadow-2xl",
            "animate-fade-in-up animation-delay-700",
          )}
          asChild
        >
          <Link href="/booking/new">
            <span className="mr-2 inline-block text-lg animate-float-gentle" aria-hidden>
              ⚡
            </span>
            Đặt sân nhanh
          </Link>
        </Button>
      </div>
    </section>
  );
}
