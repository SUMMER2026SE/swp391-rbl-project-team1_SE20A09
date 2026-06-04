"use client";

/** Nền gradient + orbs + lưới nhẹ cho trang chủ đã đăng nhập */
export function HomeAmbientBackground() {
  return (
    <div
      className="pointer-events-none fixed inset-0 -z-10 overflow-hidden"
      aria-hidden
    >
      <div className="absolute inset-0 bg-gradient-to-b from-emerald-50/90 via-background to-amber-50/30" />
      <div className="home-grid-pattern absolute inset-0 opacity-[0.35]" />
      <div className="animate-orb-float absolute -left-32 top-20 h-72 w-72 rounded-full bg-emerald-400/25 blur-3xl" />
      <div className="animate-orb-float animation-delay-2000 absolute right-0 top-1/3 h-96 w-96 rounded-full bg-teal-300/20 blur-3xl" />
      <div className="animate-orb-float animation-delay-4000 absolute bottom-20 left-1/3 h-64 w-64 rounded-full bg-amber-300/20 blur-3xl" />
      <div className="animate-shimmer-sweep absolute inset-0 bg-[linear-gradient(105deg,transparent_40%,rgba(255,255,255,0.12)_50%,transparent_60%)] bg-[length:200%_100%]" />
    </div>
  );
}
