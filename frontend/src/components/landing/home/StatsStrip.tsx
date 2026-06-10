import { CalendarCheck, MapPin, Star, Trophy } from "lucide-react";
import { HOME_STATS } from "@/lib/home-data";
import { cn } from "@/lib/utils";

const ICONS = {
  venues: MapPin,
  bookings: CalendarCheck,
  rating: Star,
  cities: Trophy,
} as const;

type StatsStripProps = {
  className?: string;
  variant?: "hero" | "default";
};

export function StatsStrip({ className, variant = "default" }: StatsStripProps) {
  const isHero = variant === "hero";

  return (
    <div
      className={cn(
        "grid grid-cols-2 gap-3 md:grid-cols-4 md:gap-4",
        className,
      )}
    >
      {HOME_STATS.map((stat) => {
        const Icon = ICONS[stat.icon];
        return (
          <div
            key={stat.label}
            className={cn(
              "rounded-2xl border p-4 text-center transition-all duration-300",
              isHero
                ? "border-white/40 bg-white/15 text-white backdrop-blur-md hover:bg-white/15"
                : "border-border bg-card hover:border-primary/40 hover:shadow-md",
            )}
          >
            <Icon
              className={cn(
                "mx-auto mb-2 h-5 w-5",
                isHero ? "text-emerald-700" : "text-primary",
              )}
            />
            <p
              className={cn(
                "text-2xl font-bold tracking-tight",
                isHero ? "text-emerald-500" : "text-foreground",
              )}
            >
              {stat.value}
            </p>
            <p
              className={cn(
                "mt-0.5 text-xs font-medium",
                isHero ? "text-black" : "text-muted-foreground",
              )}
            >
              {stat.label}
            </p>
          </div>
        );
      })}
    </div>
  );
}
