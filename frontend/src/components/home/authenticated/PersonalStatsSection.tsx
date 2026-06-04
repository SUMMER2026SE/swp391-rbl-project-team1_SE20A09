import { Clock, MapPinned, Trophy } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";

type PersonalStatsSectionProps = {
  totalHours: number;
  venuesVisited: number;
  favoriteSport: string;
};

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
      color: "text-blue-600 bg-blue-50",
    },
    {
      icon: MapPinned,
      value: String(venuesVisited),
      label: "Sân đã ghé thăm",
      color: "text-green-700 bg-green-50",
    },
    {
      icon: Trophy,
      value: favoriteSport,
      label: "Môn hay chơi nhất",
      color: "text-amber-600 bg-amber-50",
    },
  ];

  return (
    <section className="pb-12 pt-4 md:pb-16">
      <div className="container mx-auto px-4">
        <h2 className="mb-6 text-2xl font-bold">Thống kê cá nhân</h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          {stats.map((stat) => (
            <Card key={stat.label} className="border-border/80">
              <CardContent className="flex items-center gap-4 p-5">
                <div
                  className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-xl ${stat.color}`}
                >
                  <stat.icon className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-2xl font-bold leading-none">{stat.value}</p>
                  <p className="mt-1 text-sm text-muted-foreground">{stat.label}</p>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    </section>
  );
}
