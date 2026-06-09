"use client";

const SPARKLES = [
  { top: "8%", left: "12%", delay: "0s", size: "h-1 w-1" },
  { top: "22%", left: "78%", delay: "1.2s", size: "h-1.5 w-1.5" },
  { top: "45%", left: "5%", delay: "2.4s", size: "h-1 w-1" },
  { top: "60%", left: "92%", delay: "0.8s", size: "h-2 w-2" },
  { top: "75%", left: "35%", delay: "1.8s", size: "h-1 w-1" },
  { top: "15%", left: "55%", delay: "3s", size: "h-1.5 w-1.5" },
];

/** Hạt lấp lánh trang trí (CSS only) */
export function SparkleField({ className = "" }: { className?: string }) {
  return (
    <div
      className={`pointer-events-none absolute inset-0 overflow-hidden ${className}`}
      aria-hidden
    >
      {SPARKLES.map((s, i) => (
        <span
          key={i}
          className={`animate-sparkle absolute rounded-full bg-amber-200/90 shadow-[0_0_8px_2px_rgba(251,191,36,0.5)] ${s.size}`}
          style={{ top: s.top, left: s.left, animationDelay: s.delay }}
        />
      ))}
    </div>
  );
}
