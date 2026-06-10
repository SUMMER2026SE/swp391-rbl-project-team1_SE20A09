"use client";

import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export function HomeCtaBanner() {
  return (
    <section className="py-16 md:py-20">
      <div className="container mx-auto px-4">
        <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-primary via-emerald-600 to-teal-700 px-8 py-14 text-center shadow-2xl shadow-primary/20 md:px-16 md:py-20">
          <div className="absolute -right-20 -top-20 h-64 w-64 rounded-full bg-white/10 blur-2xl" />
          <div className="absolute -bottom-16 -left-16 h-48 w-48 rounded-full bg-black/10 blur-2xl" />

          <div className="relative z-10 mx-auto max-w-2xl">
            <h2 className="text-3xl font-bold text-white md:text-4xl">
              Sẵn sàng cho trận đấu tiếp theo?
            </h2>
            <p className="mt-4 text-lg text-emerald-50/90">
              Đăng ký miễn phí để lưu sân yêu thích, nhận ưu đãi và đặt lịch chỉ trong
              vài giây.
            </p>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
              <Button
                size="lg"
                className="h-12 rounded-full bg-white px-8 text-primary hover:bg-emerald-50"
                asChild
              >
                <Link href="/register">
                  Bắt đầu ngay
                  <ArrowRight className="ml-2 h-5 w-5" />
                </Link>
              </Button>
              <Button
                size="lg"
                variant="outline"
                className="h-12 rounded-full border-white/40 bg-transparent px-8 text-white hover:bg-white/10 hover:text-white"
                asChild
              >
                <Link href="/search">Chỉ xem sân</Link>
              </Button>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
