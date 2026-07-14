"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ChevronLeft, ChevronRight, ArrowRight, UserPlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { searchComplexes } from "@/lib/api/complex";
import type { StadiumComplexDto } from "@/types/complex";

type HeroSlide = {
  id: number;
  image: string;
  tag: string;
  title: string;
  desc: string;
  href: string;
};

// Dùng khi chưa tải xong dữ liệu thật, hoặc fetch lỗi/DB chưa có complex nào đủ
// review — tránh trang chủ trống trơn trong lúc chờ hoặc khi gặp sự cố.
const FALLBACK_SLIDES: HeroSlide[] = [
  {
    id: -1,
    image:
      "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?q=80&w=2000&auto=format&fit=crop",
    tag: "Khám phá",
    title: "Đặt sân thể thao dễ dàng",
    desc: "Tìm và đặt sân bóng đá, cầu lông, tennis... nhanh chóng chỉ với vài bước.",
    href: "/search",
  },
];

function toHeroSlide(complex: StadiumComplexDto): HeroSlide {
  const sportLabel = complex.sportTypes?.[0]?.sportName;
  return {
    id: complex.complexId,
    image: complex.coverImageUrl || FALLBACK_SLIDES[0].image,
    tag:
      complex.reviewCount && complex.reviewCount > 0
        ? `⭐ ${complex.averageRating.toFixed(1)} (${complex.reviewCount} đánh giá)`
        : "Nổi bật",
    title: complex.name,
    desc: [sportLabel, complex.address].filter(Boolean).join(" • "),
    href: `/complexes/${complex.complexId}?tab=courts`,
  };
}

export function GuestHeroSlider() {
  // Ngắn hạn: giữ hiển thị dạng slider tĩnh, nhưng nội dung lấy từ 3 complex
  // có rating cao nhất thay vì tag/title/desc/ảnh Unsplash giả lập trước đây.
  const [slides, setSlides] = useState<HeroSlide[]>(FALLBACK_SLIDES);
  const [currentSlide, setCurrentSlide] = useState(0);
  const sliderRef = useRef<HTMLDivElement>(null);
  const router = useRouter();

  useEffect(() => {
    let cancelled = false;
    searchComplexes({ size: 30, page: 0 })
      .then((res) => {
        if (cancelled) return;
        const topRated = [...res.content]
          .filter((c) => (c.reviewCount ?? 0) > 0)
          .sort((a, b) => b.averageRating - a.averageRating)
          .slice(0, 3);
        if (topRated.length > 0) {
          setSlides(topRated.map(toHeroSlide));
          setCurrentSlide(0);
        }
      })
      .catch(() => {
        // Giữ nguyên FALLBACK_SLIDES nếu API lỗi — không để trang chủ trống.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev === slides.length - 1 ? 0 : prev + 1));
    }, 6000);
    return () => clearInterval(timer);
  }, [slides.length]);

  useEffect(() => {
    if (sliderRef.current) {
      sliderRef.current.scrollTo({
        left: currentSlide * sliderRef.current.offsetWidth,
        behavior: "smooth",
      });
    }
  }, [currentSlide]);

  const goToSlide = (index: number) => setCurrentSlide(index);
  const nextSlide = () =>
    setCurrentSlide((prev) => (prev === slides.length - 1 ? 0 : prev + 1));
  const prevSlide = () =>
    setCurrentSlide((prev) => (prev === 0 ? slides.length - 1 : prev - 1));

  return (
    <section className="w-full">
      {/* FULL WIDTH HERO SLIDER (Edge to Edge) */}
      <div className="relative w-full h-[500px] md:h-[600px] lg:h-[75vh] max-h-[800px] overflow-hidden group bg-slate-900">

        {/* SLIDER TRACK */}
        <div
          ref={sliderRef}
          className="flex h-full w-full overflow-x-hidden snap-x snap-mandatory scroll-smooth"
        >
          {slides.map((slide) => (
            <div
              key={slide.id}
              className="relative w-full h-full flex-shrink-0 snap-center cursor-pointer"
              onClick={() => router.push(slide.href)}
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={slide.image}
                alt={slide.title}
                className="absolute inset-0 w-full h-full object-cover transition-transform duration-[15000ms] ease-linear hover:scale-105"
              />

              {/* Dark Gradient Overlay */}
              <div className="absolute inset-0 bg-gradient-to-r from-black/80 via-black/40 to-transparent" />

              {/* Slide Content */}
              <div className="absolute inset-0 flex items-center">
                <div className="container mx-auto px-6 sm:px-12 md:px-16 lg:px-20 max-w-7xl">
                  <div className="max-w-3xl">
                    <span className="inline-block px-4 py-1.5 bg-emerald-500 text-white text-xs font-bold uppercase tracking-widest rounded-full mb-6 shadow-lg">
                      {slide.tag}
                    </span>
                    <h1 className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-extrabold text-white mb-6 leading-[1.1] drop-shadow-2xl">
                      {slide.title}
                    </h1>
                    <p className="text-lg sm:text-xl md:text-2xl text-slate-200 mb-10 max-w-2xl drop-shadow-xl leading-relaxed font-medium">
                      {slide.desc}
                    </p>

                    {/* CTA Buttons — chặn bubble để không double-navigate với onClick của slide cha */}
                    <div
                      className="flex flex-wrap items-center gap-4"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <Button
                        size="lg"
                        className="h-13 rounded-full bg-white px-8 text-emerald-900 shadow-xl hover:bg-emerald-50 font-bold text-base"
                        asChild
                      >
                        <Link href={slide.href}>
                          Khám phá sân ngay
                          <ArrowRight className="ml-2 h-5 w-5" />
                        </Link>
                      </Button>
                      <Button
                        size="lg"
                        variant="outline"
                        className="h-13 rounded-full border-white/30 bg-white/10 px-8 text-white backdrop-blur-sm hover:bg-white/20 font-bold text-base"
                        asChild
                      >
                        <Link href="/register">
                          <UserPlus className="mr-2 h-5 w-5" />
                          Đăng ký miễn phí
                        </Link>
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Navigation Arrows */}
        <button
          onClick={prevSlide}
          aria-label="Slide trước"
          className="absolute left-4 md:left-8 top-1/2 -translate-y-1/2 w-14 h-14 rounded-full bg-black/30 backdrop-blur-md hover:bg-black/60 border border-white/20 flex items-center justify-center text-white opacity-0 group-hover:opacity-100 transition-all hover:scale-110 z-10"
        >
          <ChevronLeft className="w-8 h-8" />
        </button>
        <button
          onClick={nextSlide}
          aria-label="Slide tiếp theo"
          className="absolute right-4 md:right-8 top-1/2 -translate-y-1/2 w-14 h-14 rounded-full bg-black/30 backdrop-blur-md hover:bg-black/60 border border-white/20 flex items-center justify-center text-white opacity-0 group-hover:opacity-100 transition-all hover:scale-110 z-10"
        >
          <ChevronRight className="w-8 h-8" />
        </button>

        {/* Dots Indicator */}
        <div className="absolute bottom-8 left-0 right-0 flex justify-center gap-3 z-10">
          {slides.map((_, i) => (
            <button
              key={i}
              onClick={() => goToSlide(i)}
              aria-label={`Chuyển sang slide ${i + 1}`}
              className={`h-3 rounded-full transition-all duration-500 ease-out ${
                currentSlide === i
                  ? "bg-emerald-500 w-12 shadow-[0_0_15px_rgba(16,185,129,0.8)]"
                  : "bg-white/50 hover:bg-white w-3"
              }`}
            />
          ))}
        </div>
      </div>
    </section>
  );
}
