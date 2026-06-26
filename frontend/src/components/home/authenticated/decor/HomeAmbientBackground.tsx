"use client";

/** Nền gradient + orbs + lưới nhẹ cho trang chủ đã đăng nhập */
export function HomeAmbientBackground() {
  return (
    <div
      className="pointer-events-none fixed inset-0 -z-10 overflow-hidden bg-slate-50"
      aria-hidden
    >
      {/* Fresh, clean sports-themed gradient base */}
      <div className="absolute inset-0 bg-gradient-to-br from-emerald-100/40 via-white to-teal-50/60" />

      {/* Dynamic, energetic Orbs matching the brand (Green/Teal/Lime) */}
      <div className="animate-orb-float absolute -top-[10%] -left-[10%] w-[50vw] h-[50vw] rounded-full bg-emerald-400/20 blur-[100px]" />
      <div className="animate-orb-float animation-delay-2000 absolute top-[20%] -right-[10%] w-[45vw] h-[45vw] rounded-full bg-teal-400/15 blur-[100px]" />
      <div className="animate-orb-float animation-delay-4000 absolute -bottom-[10%] left-[15%] w-[60vw] h-[60vw] rounded-full bg-lime-300/20 blur-[120px]" />

      {/* Modern dotted overlay (subtle and clean) */}
      <div 
        className="absolute inset-0 opacity-40" 
        style={{ 
          backgroundImage: "radial-gradient(circle at 1.5px 1.5px, rgba(16, 185, 129, 0.08) 1.5px, transparent 0)", 
          backgroundSize: "32px 32px" 
        }} 
      />
    </div>
  );
}
