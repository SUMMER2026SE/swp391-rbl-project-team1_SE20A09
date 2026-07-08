import Link from "next/link";
import { Card, CardContent } from "../ui/card";
import { Clock } from "lucide-react";
import { TimeSlotResponse } from "@/types/aiChat";

export function SlotResultCard({ slots }: { slots: TimeSlotResponse[] }) {
  if (!slots || slots.length === 0) return null;

  const stadiumId = slots[0].stadiumId;
  const href = `/venues/${stadiumId}`;

  const formatTime = (timeStr: string) => {
    if (!timeStr) return "";
    const parts = timeStr.split(":");
    return parts.length >= 2 ? `${parts[0]}:${parts[1]}` : timeStr;
  };

  return (
    <Card className="w-full max-w-md border-border/80 bg-card shadow-sm">
      <CardContent className="p-4">
        <div className="flex items-center gap-2 mb-3 text-sm font-semibold text-foreground">
          <Clock className="h-4 w-4 text-primary" />
          <span>Khung giờ trống trong ngày</span>
        </div>

        <div className="grid grid-cols-3 gap-2">
          {slots.map((slot) => {
            const isAvailable = slot.available;
            const timeText = `${formatTime(slot.startTime)} - ${formatTime(slot.endTime)}`;

            if (isAvailable) {
              return (
                <Link key={slot.slotId} href={href} className="block">
                  <div
                    className="flex flex-col items-center justify-center p-2 rounded-lg border border-primary/20 hover:border-primary hover:bg-primary/5 transition-all text-center cursor-pointer group"
                  >
                    <span className="text-xs font-medium text-foreground group-hover:text-primary transition-colors">
                      {timeText}
                    </span>
                    <span className="text-[9px] text-primary font-semibold mt-0.5">
                      {slot.pricePerSlot.toLocaleString("vi-VN")}đ
                    </span>
                  </div>
                </Link>
              );
            }

            return (
              <div
                key={slot.slotId}
                className="flex flex-col items-center justify-center p-2 rounded-lg border border-border bg-muted/40 opacity-40 text-center select-none"
              >
                <span className="text-xs font-medium text-muted-foreground">
                  {timeText}
                </span>
                <span className="text-[9px] text-muted-foreground mt-0.5">
                  Đã đặt
                </span>
              </div>
            );
          })}
        </div>
      </CardContent>
    </Card>
  );
}
