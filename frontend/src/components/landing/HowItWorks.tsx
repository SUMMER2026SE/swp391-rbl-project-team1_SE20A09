"use client";

import { Calendar, CheckCircle, Search } from "lucide-react";

export function HowItWorks() {
  return (
    <section className="py-20 md:py-28">
      <div className="container mx-auto px-4">
        <div className="mx-auto mb-16 max-w-2xl text-center">
          <p className="text-sm font-semibold uppercase tracking-widest text-primary">
            {"Cách thức hoạt động"}
          </p>
          <h2 className="mt-2 text-3xl font-bold tracking-tight md:text-4xl">
            {"Ba bước đơn giản để có sân"}
          </h2>
          <p className="mt-4 text-lg text-muted-foreground">
            {"Khách có thể xem sân ngay; đăng ký khi sẵn sàng đặt lịch."}
          </p>
        </div>

        <div className="mx-auto grid max-w-5xl grid-cols-1 gap-12 md:grid-cols-3 md:gap-10">
          <div className="flex flex-col items-center text-center">
            <div className="relative mb-6">
              <div className="flex h-24 w-24 items-center justify-center rounded-2xl bg-primary/10 ring-1 ring-primary/20">
                <Search className="h-11 w-11 text-primary" />
              </div>
              <span className="absolute -right-1 -top-1 flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-bold text-primary-foreground">
                1
              </span>
            </div>
            <h3 className="text-xl font-semibold">{"Tìm kiếm sân"}</h3>
            <p className="mt-2 max-w-xs text-muted-foreground">
              {"Lọc theo vị trí, môn thể thao, ngày và khung giờ phù hợp với bạn"}
            </p>
          </div>

          <div className="flex flex-col items-center text-center">
            <div className="relative mb-6">
              <div className="flex h-24 w-24 items-center justify-center rounded-2xl bg-primary/10 ring-1 ring-primary/20">
                <Calendar className="h-11 w-11 text-primary" />
              </div>
              <span className="absolute -right-1 -top-1 flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-bold text-primary-foreground">
                2
              </span>
            </div>
            <h3 className="text-xl font-semibold">{"Đặt lịch và thanh toán"}</h3>
            <p className="mt-2 max-w-xs text-muted-foreground">
              {"Chọn slot còn trống, xác nhận và thanh toán trực tuyến an toàn"}
            </p>
          </div>

          <div className="flex flex-col items-center text-center">
            <div className="relative mb-6">
              <div className="flex h-24 w-24 items-center justify-center rounded-2xl bg-primary/10 ring-1 ring-primary/20">
                <CheckCircle className="h-11 w-11 text-primary" />
              </div>
              <span className="absolute -right-1 -top-1 flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-bold text-primary-foreground">
                3
              </span>
            </div>
            <h3 className="text-xl font-semibold">{"Ra sân và tận hưởng"}</h3>
            <p className="mt-2 max-w-xs text-muted-foreground">
              {"Nhận xác nhận qua app và tận hưởng trận đấu của bạn"}
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
