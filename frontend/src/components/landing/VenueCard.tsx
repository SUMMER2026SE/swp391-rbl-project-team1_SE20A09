'use client'

import Link from "next/link";
import { Star, MapPin, Clock, Sparkles, Heart } from "lucide-react";
import { Card, CardContent, CardFooter } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
import Image from "next/image";

export interface VenueCardProps {
  id?: number;
  image: string;
  name: string;
  /** Tên khu phức hợp chứa sân — undefined/null nếu sân độc lập. */
  complexName?: string | null;
  sportType: string;
  price: number;
  rating: number;
  location: string;
  reviewCount?: number;
  featured?: boolean;
  saved?: boolean;
  openHours?: string;
  actionLabel?: string;
  actionHref?: string;
}

export function VenueCard({
  id,
  image,
  name,
  complexName,
  sportType,
  price,
  rating,
  location,
  reviewCount,
  featured,
  saved,
  openHours,
  actionLabel = "Xem & đặt sân",
  actionHref,
}: VenueCardProps) {
  const href = actionHref ?? (id ? `/venues/${id}` : "/search");

  return (
    <Link href={href} className="block group h-full">
      <Card className="overflow-hidden border-border/80 transition-all duration-300 hover:-translate-y-1 hover:border-primary/25 hover:shadow-xl hover:shadow-primary/10 h-full flex flex-col justify-between">
        <div>
          <div className="relative h-52 overflow-hidden bg-emerald-50">
            {image ? (
              <Image
                src={image}
                alt={name}
                fill
                className="object-cover transition-transform duration-500 group-hover:scale-110"
                unoptimized
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-emerald-200 font-medium">
                No Image
              </div>
            )}
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
            <Badge className="absolute right-3 top-3 border-0 bg-primary/95 shadow-md">
              {sportType}
            </Badge>
            {saved && (
              <Badge
                variant="secondary"
                className="absolute left-3 top-3 gap-1 border-0 bg-rose-500/95 text-white shadow-md"
              >
                <Heart className="h-3 w-3 fill-current" />
                Đã lưu
              </Badge>
            )}
            {featured && !saved && (
              <Badge
                variant="secondary"
                className="absolute left-3 top-3 gap-1 border-0 bg-amber-400/95 text-amber-950 shadow-md"
              >
                <Sparkles className="h-3 w-3" />
                Nổi bật
              </Badge>
            )}
          </div>

          <CardContent className="p-5">
            <h3 className="text-lg font-semibold leading-snug line-clamp-1">{name}</h3>
            {complexName && (
              <p className="text-xs text-muted-foreground line-clamp-1">{complexName}</p>
            )}

            <div className="mt-2 flex items-center text-sm text-muted-foreground">
              <MapPin className="mr-1.5 h-4 w-4 shrink-0 text-primary/70" />
              <span className="line-clamp-1">{location}</span>
            </div>

            {openHours && (
              <div className="mt-1.5 flex items-center text-xs text-muted-foreground">
                <Clock className="mr-1.5 h-3.5 w-3.5" />
                {openHours}
              </div>
            )}

            <div className="mt-4 flex items-end justify-between gap-2">
              <div className="flex items-center gap-1.5">
                <Star className="h-4 w-4 fill-amber-400 text-amber-400" />
                <span className="font-semibold">{rating.toFixed(1)}</span>
                {reviewCount != null && (
                  <span className="text-xs text-muted-foreground">({reviewCount})</span>
                )}
              </div>
              <div className="text-right">
                <span className="font-bold text-primary">
                  {(price ?? 0).toLocaleString("vi-VN")}đ
                </span>
                <span className="text-xs text-muted-foreground">/giờ</span>
              </div>
            </div>
          </CardContent>
        </div>

        <CardFooter className="p-5 pt-0">
          <Button className="w-full rounded-xl font-semibold" type="button">
            {actionLabel}
          </Button>
        </CardFooter>
      </Card>
    </Link>
  );
}
