"use client";

import { Clock, MapPinned, Trophy } from "lucide-react";
import { CardContent } from "@/components/ui/card";
import { SectionHeading } from "@/components/home/authenticated/decor/SectionHeading";
import { GlowCard } from "@/components/home/authenticated/decor/GlowCard";
import { cn } from "@/lib/utils";

type PersonalStatsSectionProps = {
  totalHours: number;
  venuesVisited: number;
  favoriteSport: string;
};

const DELAYS = ["animation-delay-300", "animation-delay-500", "animation-delay-700"];

export function PersonalStatsSection({
  totalHours,
  venuesVisited,
  favoriteSport,
}: PersonalStatsSectionProps) {
  const stats = [
    {
      icon: Clock,
      value: `${totalHours}h`,
      label: "Tổng giờ chơi",
      // Soft, elegant blue shimmer
      gradient: "from-blue-500/10 via-cyan-400/5 to-blue-500/10",
      iconColor: "text-blue-600 bg-blue-100",
    },
    {
      icon: MapPinned,
      value: String(venuesVisited),
      label: "Sân đã ghé thăm",
      // Soft, elegant green shimmer
      gradient: "from-emerald-500/10 via-green-400/5 to-emerald-500/10",
      iconColor: "text-green-700 bg-green-100",
    },
    {
      icon: Trophy,
      value: favoriteSport,
      label: "Môn hay chơi nhất",
      // Soft, elegant yellow shimmer
      gradient: "from-amber-500/10 via-yellow-400/5 to-amber-500/10",
      iconColor: "text-amber-600 bg-amber-100",
    },
  ];

  return (
    <section className="relative pb-14 pt-4 md:pb-20">
      <div className="container mx-auto px-4">
        <SectionHeading
          title="Thống kê cá nhân"
          subtitle="Hành trình thể thao của bạn qua từng buổi chơi"
        />
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-3">
          {stats.map((stat, index) => (
            <GlowCard key={stat.label} delayClass={DELAYS[index]}>
              <CardContent
                className={cn(
                  "home-stat-glow relative overflow-hidden p-6 cursor-pointer",
                  `bg-gradient-to-r ${stat.gradient}`,
                  "bg-[length:200%_auto] animate-[text-shimmer_5s_linear_infinite]"
                )}
              >
                <div className="absolute -right-4 -top-4 h-24 w-24 rounded-full bg-white/40 blur-2xl" />
                <div className="relative flex items-center gap-4 transition-transform duration-300 hover:scale-[1.02]">
                  <div
                    className={cn(
                      "flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl shadow-inner",
                      stat.iconColor,
                    )}
                  >
                    <stat.icon className="h-7 w-7" />
                  </div>
                  <div>
                    <p className="text-3xl font-bold tracking-tight text-shimmer">
                      {stat.value}
                    </p>
                    <p className="mt-1 text-sm font-medium text-muted-foreground">
                      {stat.label}
                    </p>
                  </div>
                </div>
              </CardContent>
            </GlowCard>
          ))}
        </div>
      </div>
    </section>
  );
}
