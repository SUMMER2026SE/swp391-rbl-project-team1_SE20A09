"use client";

import { Clock, MapPinned, Trophy } from "lucide-react";
import { GlowCard } from "@/components/home/authenticated/decor/GlowCard";
import { SectionHeading } from "@/components/home/authenticated/decor/SectionHeading";
import { cn } from "@/lib/utils";

type PersonalStatsSectionProps = {
  totalHours: number;
  venuesVisited: number;
  favoriteSport: string;
};

const THEMES = {
  blue: {
    // Soft, clean pastel Blue to Pink
    bg: "bg-white/40 bg-gradient-to-r from-blue-300/30 via-fuchsia-200/30 to-blue-300/30 border-[#bfdbfe]",
    textValue: "text-[#2563eb]",
    textLabel: "text-[#64748b]",
    iconWrapper: "bg-[#ffffff] border-[#bfdbfe] shadow-sm shadow-blue-200/50 text-[#2563eb]",
  },
  emerald: {
    // Soft, clean pastel Green to Yellow
    bg: "bg-white/40 bg-gradient-to-r from-emerald-300/30 via-amber-200/30 to-emerald-300/30 border-[#a7f3d0]",
    textValue: "text-[#059669]",
    textLabel: "text-[#64748b]",
    iconWrapper: "bg-[#ffffff] border-[#a7f3d0] shadow-sm shadow-emerald-200/50 text-[#059669]",
  },
  amber: {
    // Soft, clean pastel Orange to Rose
    bg: "bg-white/40 bg-gradient-to-r from-orange-300/30 via-rose-200/30 to-orange-300/30 border-[#fde68a]",
    textValue: "text-[#d97706]",
    textLabel: "text-[#64748b]",
    iconWrapper: "bg-[#ffffff] border-[#fde68a] shadow-sm shadow-amber-200/50 text-[#d97706]",
  }
};

function BlueDecor() {
  return (
    <div className="absolute bottom-0 right-0 w-1/2 h-full pointer-events-none overflow-hidden rounded-r-[inherit]">
      <svg viewBox="0 0 100 100" preserveAspectRatio="none" className="absolute right-0 bottom-0 w-full h-full text-blue-200/50">
        <path d="M30,100 C50,60 80,70 100,20 L100,100 Z" fill="currentColor" />
        <path d="M0,100 C30,70 60,80 100,50 L100,100 Z" fill="currentColor" className="opacity-50" />
        <path d="M10,80 C40,40 70,50 100,20" fill="none" stroke="currentColor" strokeWidth="1.5" strokeDasharray="3 4" className="text-blue-400/40" />
        <circle cx="85" cy="35" r="3" fill="currentColor" className="text-blue-500/60" />
      </svg>
    </div>
  );
}

function EmeraldDecor() {
  return (
    <div className="absolute bottom-0 right-0 w-1/2 h-full pointer-events-none overflow-hidden rounded-r-[inherit]">
      <svg viewBox="0 0 100 100" preserveAspectRatio="none" className="absolute right-0 bottom-0 w-full h-full text-emerald-200/50">
        <path d="M10,100 C30,70 60,90 100,40 L100,100 Z" fill="currentColor" />
        <path d="M-10,100 C20,60 50,80 100,20 L100,100 Z" fill="currentColor" className="opacity-50" />
        <path d="M85,35 C81,35 79,38 79,42 C79,48 85,55 85,55 C85,55 91,48 91,42 C91,38 89,35 85,35 Z" fill="currentColor" className="text-emerald-500/70" />
        <circle cx="85" cy="40.5" r="2.5" fill="white" />
        <circle cx="70" cy="60" r="1.5" fill="currentColor" className="text-emerald-400/50" />
        <circle cx="90" cy="70" r="1" fill="currentColor" className="text-emerald-400/50" />
      </svg>
    </div>
  );
}

function AmberDecor() {
  return (
    <div className="absolute bottom-0 right-0 w-1/2 h-full pointer-events-none overflow-hidden rounded-r-[inherit]">
      <svg viewBox="0 0 100 100" preserveAspectRatio="none" className="absolute right-0 bottom-0 w-full h-full text-amber-200/50">
        <path d="M20,100 C50,70 70,90 100,30 L100,100 Z" fill="currentColor" />
        <path d="M0,100 C30,60 60,80 100,20 L100,100 Z" fill="currentColor" className="opacity-50" />
        <circle cx="80" cy="40" r="16" fill="none" stroke="currentColor" strokeWidth="2" className="text-amber-400/50"/>
        <path d="M68,33 L80,40 L92,33 M68,47 L80,40 L92,47" fill="none" stroke="currentColor" strokeWidth="1.5" className="text-amber-400/50" />
      </svg>
    </div>
  );
}

const STAGGER = ["animation-delay-300", "animation-delay-500", "animation-delay-700"];

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
      theme: THEMES.blue,
      decor: <BlueDecor />
    },
    {
      icon: MapPinned,
      value: String(venuesVisited),
      label: "Sân đã ghé thăm",
      theme: THEMES.emerald,
      decor: <EmeraldDecor />
    },
    {
      icon: Trophy,
      value: favoriteSport,
      label: "Môn hay chơi nhất",
      theme: THEMES.amber,
      decor: <AmberDecor />
    },
  ];

  return (
    <section className="relative overflow-hidden pb-14 pt-8 md:pb-20">
      
      {/* Light dotted grid background (transparent base) */}
      <div className="absolute inset-0 bg-[radial-gradient(#cbd5e1_1px,transparent_1px)] [background-size:24px_24px] opacity-40"></div>
      
      {/* Subtle Football Watermark Top Right */}
      <div className="absolute right-[-5%] top-[-10%] opacity-[0.03] pointer-events-none md:right-[5%] md:top-[-5%]">
        <svg width="400" height="400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="0.3" className="text-emerald-900">
          <circle cx="12" cy="12" r="11" />
          <path d="M12 7.5 L15.5 10 L14.5 14.5 L9.5 14.5 L8.5 10 Z" />
          <path d="M12 7.5 L12 1" />
          <path d="M15.5 10 L21 8" />
          <path d="M14.5 14.5 L18 20" />
          <path d="M9.5 14.5 L6 20" />
          <path d="M8.5 10 L3 8" />
        </svg>
      </div>

      <div className="container relative mx-auto px-4 z-10">
        
        {/* Animated Section Title */}
        <SectionHeading
          title="Thống kê cá nhân"
          subtitle="Hành trình thể thao của bạn qua từng buổi chơi"
        />

        {/* Stats Grid */}
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-3 mt-4">
          {stats.map((stat, index) => (
            <GlowCard key={stat.label} delayClass={STAGGER[index]}>
              <div
                className={cn(
                  "home-stat-glow relative flex h-full flex-col justify-center overflow-hidden rounded-[inherit] border p-6 backdrop-blur-md cursor-pointer",
                  "bg-[length:200%_auto] animate-[text-shimmer_5s_linear_infinite]",
                  stat.theme.bg
                )}
              >
                {/* Background SVG Decor */}
                {stat.decor}

                <div className="relative z-10 flex items-center gap-5 transition-transform duration-300 hover:scale-[1.02]">
                  {/* Icon with Double Ring */}
                  <div className={cn(
                    "relative flex h-16 w-16 shrink-0 items-center justify-center rounded-full border-2",
                    stat.theme.iconWrapper
                  )}>
                    <div className="absolute inset-1.5 rounded-full border border-dashed border-current opacity-30"></div>
                    <stat.icon className="h-7 w-7 drop-shadow-sm" strokeWidth={2} />
                  </div>
                  
                  {/* Content */}
                  <div>
                    <p className={cn("text-3xl font-black tracking-tight text-shimmer", stat.theme.textValue)}>
                      {stat.value}
                    </p>
                    <p className={cn("mt-1 text-sm font-semibold tracking-wide", stat.theme.textLabel)}>
                      {stat.label}
                    </p>
                  </div>
                </div>
              </div>
            </GlowCard>
          ))}
        </div>
      </div>
    </section>
  );
}
