"use client";

import Link from "next/link";
import { ArrowRight, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { SearchBar } from "@/components/landing/SearchBar";
import { StatsStrip } from "@/components/landing/home/StatsStrip";

export function HeroSection() {
  return (
    <section className="relative min-h-[88vh] flex flex-col justify-center overflow-hidden">
      {/* Background layers */}
      <div
        className="absolute inset-0 bg-cover bg-center bg-no-repeat scale-105"
        style={{
          backgroundImage:
            "url(https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=1920&q=80)",
        }}
      />
      <div className="absolute inset-0 bg-gradient-to-b from-emerald-950/85 via-emerald-900/75 to-background" />
      <div className="absolute inset-0 home-grid-pattern opacity-[0.07]" />

      {/* Ambient orbs */}
      <div className="absolute top-1/4 -left-32 h-96 w-96 rounded-full bg-emerald-400/20 blur-3xl animate-pulse-slow" />
      <div className="absolute bottom-1/4 -right-32 h-80 w-80 rounded-full bg-teal-300/15 blur-3xl animate-pulse-slow animation-delay-700" />

      <div className="container relative z-10 mx-auto px-4 py-16 md:py-24">
        <div className="mx-auto max-w-4xl text-center">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-1.5 text-sm text-emerald-50 backdrop-blur-md">
            <Sparkles className="h-4 w-4 text-emerald-300" />
            <span>SportHub - Điểm đến của người yêu thể thao</span>
          </div>

          <h1 className="text-4xl font-extrabold tracking-tight text-white sm:text-5xl md:text-6xl lg:text-7xl">
            Khám phá sân thể thao
            <br />
            <span className="text-3xl md:text-4xl bg-gradient-to-r from-emerald-200 via-green-100 to-teal-200 bg-clip-text text-transparent">
              Tìm sân&nbsp;&nbsp;•&nbsp;&nbsp;Đặt sân&nbsp;&nbsp;•&nbsp;&nbsp;Kết nối cộng đồng
            </span>
          </h1>

          <p className="mx-auto mt-6 max-w-2xl text-lg text-emerald-50/90 md:text-xl leading-relaxed">
            Khám phá các sân thể thao chất lượng, đặt sân dễ dàng và tham gia cộng đồng năng động.
          </p>

          <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
            <Button
              size="lg"
              className="h-12 rounded-full bg-white px-8 text-emerald-900 shadow-xl hover:bg-emerald-50"
              asChild
            >
              <Link href="/search">
                Khám phá sân ngay
                <ArrowRight className="ml-2 h-5 w-5" />
              </Link>
            </Button>
            <Button
              size="lg"
              variant="outline"
              className="h-12 rounded-full border-white/30 bg-white/5 px-8 text-white backdrop-blur-sm hover:bg-white/15 hover:text-emerald-900"
              asChild
            >
              <Link href="/register">Đăng ký miễn phí</Link>
            </Button>
          </div>
        </div>

        <div className="relative mx-auto mt-14 max-w-5xl">
          <SearchBar variant="hero" />
        </div>

        <StatsStrip className="mt-16" variant="hero" />
      </div>

      <div className="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-emerald/90 to-transparent" />
    </section>
  );
}
