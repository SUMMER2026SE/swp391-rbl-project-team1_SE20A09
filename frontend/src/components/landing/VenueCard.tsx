'use client'

import { Star, MapPin } from "lucide-react";
import { Card, CardContent, CardFooter } from "../ui/card";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";

interface VenueCardProps {
  image: string;
  name: string;
  sportType: string;
  price: number;
  rating: number;
  location: string;
}

export function VenueCard({
  image,
  name,
  sportType,
  price,
  rating,
  location,
}: VenueCardProps) {
  return (
    <Card className="overflow-hidden hover:shadow-lg transition-shadow">
      <div className="relative h-48 overflow-hidden">
        <img
          src={image}
          alt={name}
          className="w-full h-full object-cover"
        />
        <Badge className="absolute top-3 right-3 bg-primary">
          {sportType}
        </Badge>
      </div>

      <CardContent className="p-4">
        <h3 className="mb-2">{name}</h3>

        <div className="flex items-center text-sm text-muted-foreground mb-2">
          <MapPin className="h-4 w-4 mr-1" />
          {location}
        </div>

        <div className="flex items-center justify-between">
          <div className="flex items-center">
            <Star className="h-4 w-4 text-yellow-500 fill-yellow-500 mr-1" />
            <span>{rating.toFixed(1)}</span>
          </div>
          <div>
            <span className="text-primary">Từ {price.toLocaleString('vi-VN')}đ</span>
            <span className="text-muted-foreground text-sm">/giờ</span>
          </div>
        </div>
      </CardContent>

      <CardFooter className="p-4 pt-0">
        <Button className="w-full">Đặt ngay</Button>
      </CardFooter>
    </Card>
  );
}

