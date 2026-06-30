"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { ChevronLeft, ChevronRight, ArrowRight, UserPlus } from "lucide-react";
import { Button } from "@/components/ui/button";

const SLIDES = [
  {
    id: 1,
    image:
      "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?q=80&w=2000&auto=format&fit=crop",
    tag: "Khuyến mãi",
    title: "Siêu ưu đãi cuối tuần",
    desc: "Giảm 20% khi đặt sân bóng đá nhân tạo từ 18:00 - 22:00. Khám phá ngay để không bỏ lỡ!",
  },
  {
    id: 2,
    image:
      "https://images.unsplash.com/photo-1546519638-68e109498ffc?q=80&w=2000&auto=format&fit=crop",
    tag: "Sự kiện",
    title: "Giải đấu bóng rổ mùa hè",
    desc: "Đăng ký tham gia ngay để nhận phần quà hấp dẫn và giao lưu cùng các đội bóng xuất sắc.",
  },
  {
    id: 3,
    image:
      "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?q=80&w=2000&auto=format&fit=crop",
    tag: "Sân mới",
    title: "Sân cầu lông đạt chuẩn quốc tế",
    desc: "Trải nghiệm mặt sân thảm cao cấp với hệ thống ánh sáng chống lóa hoàn hảo.",
  },
];

export function GuestHeroSlider() {
  const [currentSlide, setCurrentSlide] = useState(0);
  const sliderRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev === SLIDES.length - 1 ? 0 : prev + 1));
    }, 6000);
    return () => clearInterval(timer);
  }, []);

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
    setCurrentSlide((prev) => (prev === SLIDES.length - 1 ? 0 : prev + 1));
  const prevSlide = () =>
    setCurrentSlide((prev) => (prev === 0 ? SLIDES.length - 1 : prev - 1));

  return (
    <section className="w-full">
      {/* FULL WIDTH HERO SLIDER (Edge to Edge) */}
      <div className="relative w-full h-[500px] md:h-[600px] lg:h-[75vh] max-h-[800px] overflow-hidden group bg-slate-900">

        {/* SLIDER TRACK */}
        <div
          ref={sliderRef}
          className="flex h-full w-full overflow-x-hidden snap-x snap-mandatory scroll-smooth"
        >
          {SLIDES.map((slide) => (
            <div
              key={slide.id}
              className="relative w-full h-full flex-shrink-0 snap-center"
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

                    {/* CTA Buttons */}
                    <div className="flex flex-wrap items-center gap-4">
                      <Button
                        size="lg"
                        className="h-13 rounded-full bg-white px-8 text-emerald-900 shadow-xl hover:bg-emerald-50 font-bold text-base"
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
          {SLIDES.map((_, i) => (
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
