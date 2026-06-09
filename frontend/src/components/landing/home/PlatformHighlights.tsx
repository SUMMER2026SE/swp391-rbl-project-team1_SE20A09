import {
  Bot,
  CreditCard,
  Search,
  Users,
} from "lucide-react";
import { PLATFORM_HIGHLIGHTS } from "@/lib/home-data";
import { cn } from "@/lib/utils";

const HIGHLIGHT_ICONS = {
  search: Search,
  payment: CreditCard,
  community: Users,
  ai: Bot,
} as const;

export function PlatformHighlights() {
  return (
    <section className="relative overflow-hidden border-y bg-muted/40 py-20 md:py-28">
      <div className="absolute inset-0 home-grid-pattern opacity-[0.04]" />
      <div className="container relative mx-auto px-4">
        <div className="mx-auto mb-14 max-w-2xl text-center">
          <p className="text-sm font-semibold uppercase tracking-widest text-primary">
            Giới thiệu nền tảng
          </p>
          <h2 className="mt-2 text-3xl font-bold tracking-tight md:text-4xl">
            Một ứng dụng cho mọi nhu cầu thể thao
          </h2>
          <p className="mt-4 text-lg text-muted-foreground">
            SportHub giúp bạn từ tìm sân, đặt lịch đến kết nối cộng đồng — trải nghiệm
            mượt mà cho người chơi và chủ sân.
          </p>
        </div>

        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {PLATFORM_HIGHLIGHTS.map((item, i) => {
            const Icon = HIGHLIGHT_ICONS[item.icon];
            return (
              <article
                key={item.title}
                className={cn(
                  "group relative rounded-2xl border bg-card p-6 shadow-sm transition-all duration-300",
                  "hover:-translate-y-1 hover:border-primary/30 hover:shadow-lg hover:shadow-primary/5",
                )}
                style={{ animationDelay: `${i * 100}ms` }}
              >
                <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
                  <Icon className="h-6 w-6" />
                </div>
                <h3 className="text-lg font-semibold">{item.title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                  {item.description}
                </p>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
}
