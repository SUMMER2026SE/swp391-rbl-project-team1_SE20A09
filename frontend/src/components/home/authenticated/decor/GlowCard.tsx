"use client";

import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

type GlowCardProps = {
  children: ReactNode;
  className?: string;
  delayClass?: string;
};

/** Card viền gradient + hover glow */
export function GlowCard({ children, className, delayClass = "" }: GlowCardProps) {
  return (
    <div
      className={cn(
        "group relative rounded-2xl p-[1px] transition-all duration-500",
        "bg-gradient-to-br from-emerald-400/40 via-transparent to-amber-300/30",
        "hover:from-emerald-500/60 hover:to-amber-400/50 hover:shadow-[0_8px_40px_-12px_rgba(16,185,129,0.45)]",
        "animate-fade-in-up",
        delayClass,
        className,
      )}
    >
      <div className="relative h-full overflow-hidden rounded-[15px] bg-card/95 backdrop-blur-sm">
        {children}
      </div>
    </div>
  );
}
