'use client'

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Search, MapPin, Calendar, Clock } from "lucide-react";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import { cn } from "@/lib/utils";

type SearchBarProps = {
  variant?: "default" | "hero";
};

export function SearchBar({ variant = "default" }: SearchBarProps) {
  const router = useRouter();
  const [location, setLocation] = useState("");
  const [sport, setSport] = useState("");
  const [date, setDate] = useState("");
  const [timeSlot, setTimeSlot] = useState("");

  const isHero = variant === "hero";

  const handleSearch = () => {
    const params = new URLSearchParams();
    if (location.trim()) params.set("q", location.trim());
    if (sport) params.set("sport", sport);
    if (date) params.set("date", date);
    if (timeSlot) params.set("time", timeSlot);
    const query = params.toString();
    router.push(query ? `/search?${query}` : "/search");
  };

  return (
    <div
      className={cn(
        "relative w-full rounded-2xl p-5 md:p-6",
        isHero
          ? "border border-white/20 bg-white/95 shadow-2xl shadow-black/20 backdrop-blur-xl"
          : "border bg-card shadow-lg",
      )}
    >
      <p
        className={cn(
          "mb-4 text-sm font-medium",
          isHero ? "text-emerald-900/80" : "text-muted-foreground",
        )}
      >
        Tìm kiếm nhanh — địa điểm, môn thể thao & khung giờ
      </p>

      <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-4">
        <div className="relative">
          <MapPin
            className={cn(
              "absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2",
              isHero ? "text-emerald-600/70" : "text-muted-foreground",
            )}
          />
          <Input
            placeholder="Địa điểm, quận..."
            className="h-11 rounded-xl border-border/80 pl-10"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
          />
        </div>

        <Select value={sport} onValueChange={setSport}>
          <SelectTrigger className="h-11 rounded-xl">
            <SelectValue placeholder="Loại sân" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="football">Bóng đá</SelectItem>
            <SelectItem value="badminton">Cầu lông</SelectItem>
            <SelectItem value="tennis">Quần vợt</SelectItem>
            <SelectItem value="basketball">Bóng rổ</SelectItem>
            <SelectItem value="pickleball">Pickleball</SelectItem>
          </SelectContent>
        </Select>

        <div className="relative">
          <Calendar
            className={cn(
              "absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 pointer-events-none",
              isHero ? "text-emerald-600/70" : "text-muted-foreground",
            )}
          />
          <Input
            type="date"
            className="h-11 rounded-xl pl-10"
            value={date}
            onChange={(e) => setDate(e.target.value)}
          />
        </div>

        <Select value={timeSlot} onValueChange={setTimeSlot}>
          <SelectTrigger className="h-11 rounded-xl">
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4 text-muted-foreground shrink-0" />
              <SelectValue placeholder="Khung giờ" />
            </div>
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="06-08">06:00 – 08:00</SelectItem>
            <SelectItem value="08-10">08:00 – 10:00</SelectItem>
            <SelectItem value="10-12">10:00 – 12:00</SelectItem>
            <SelectItem value="14-16">14:00 – 16:00</SelectItem>
            <SelectItem value="16-18">16:00 – 18:00</SelectItem>
            <SelectItem value="18-20">18:00 – 20:00</SelectItem>
            <SelectItem value="20-22">20:00 – 22:00</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="mt-4 flex justify-end">
        <Button
          size="lg"
          className={cn(
            "h-11 w-full rounded-xl md:w-auto md:min-w-[160px]",
            isHero && "shadow-lg shadow-primary/30",
          )}
          onClick={handleSearch}
        >
          <Search className="mr-2 h-5 w-5" />
          Tìm kiếm
        </Button>
      </div>
    </div>
  );
}
