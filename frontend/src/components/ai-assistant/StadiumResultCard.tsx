import Link from "next/link";
import { Star, MapPin } from "lucide-react";
import { Card, CardContent, CardFooter } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import Image from "next/image";
import { StadiumResponse } from "@/types/stadium";

export function StadiumResultCard({ stadium }: { stadium: StadiumResponse }) {
  const { stadiumId, stadiumName, complexName, address, averageRating, sportName, firstImageUrl, pricePerHour } = stadium;
  const href = `/venues/${stadiumId}`;

  return (
    <Link href={href} className="block group w-full max-w-sm">
      <Card className="overflow-hidden border-border/80 transition-all duration-300 hover:-translate-y-1 hover:border-primary/25 hover:shadow-xl hover:shadow-primary/10 h-full flex flex-col justify-between">
        <div>
          <div className="relative h-40 overflow-hidden bg-emerald-50">
            {firstImageUrl ? (
              <Image
                src={firstImageUrl}
                alt={stadiumName}
                fill
                className="object-cover transition-transform duration-500 group-hover:scale-110"
                unoptimized
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center bg-muted text-muted-foreground font-medium text-xs">
                Không có ảnh
              </div>
            )}
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
            <Badge className="absolute right-3 top-3 border-0 bg-primary/95 shadow-md text-[10px] px-2 py-0.5">
              {sportName}
            </Badge>
          </div>

          <CardContent className="p-4">
            <h4 className="text-sm font-semibold leading-snug line-clamp-1 group-hover:text-primary transition-colors">
              {complexName || stadiumName}
            </h4>
            {complexName && stadiumName && (
              <p className="text-xs text-muted-foreground mt-0.5 line-clamp-1">
                {stadiumName}
              </p>
            )}

            <div className="mt-1.5 flex items-center text-xs text-muted-foreground">
              <MapPin className="mr-1 h-3.5 w-3.5 shrink-0 text-primary/70" />
              <span className="line-clamp-1">{address}</span>
            </div>

            <div className="mt-3 flex items-end justify-between gap-2">
              <div className="flex items-center gap-1">
                <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
                <span className="text-xs font-semibold">{(averageRating ?? 0).toFixed(1)}</span>
              </div>
              <div className="text-right">
                <span className="font-bold text-sm text-primary">
                  {(pricePerHour ?? 0).toLocaleString("vi-VN")}đ
                </span>
                <span className="text-[10px] text-muted-foreground">/giờ</span>
              </div>
            </div>
          </CardContent>
        </div>

        <CardFooter className="p-4 pt-0">
          <Button className="w-full rounded-lg font-semibold text-xs py-1.5 h-8" type="button">
            Xem & đặt sân
          </Button>
        </CardFooter>
      </Card>
    </Link>
  );
}
