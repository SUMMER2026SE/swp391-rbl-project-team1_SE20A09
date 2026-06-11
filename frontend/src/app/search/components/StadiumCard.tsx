import { Card, CardContent, CardFooter } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { MapPin, Star, Flame } from 'lucide-react'
import { StadiumResponse } from '@/lib/api/stadium'

interface StadiumCardProps {
  stadium: StadiumResponse
  isUrgent?: boolean
}

import Image from 'next/image'
import Link from 'next/link'

export function StadiumCard({ stadium, isUrgent = false }: StadiumCardProps) {
  const mockReviewCount = Math.floor(Math.random() * 200) + 50 // Giả lập số lượng đánh giá

  return (
    <Card className="overflow-hidden bg-card hover:shadow-2xl transition-all duration-300 border-gray-100 dark:border-border group cursor-pointer flex flex-col h-full rounded-2xl">
      <div className="relative h-64 w-full bg-muted overflow-hidden">
        {stadium.firstImageUrl ? (
          <Image
            src={stadium.firstImageUrl}
            alt={stadium.stadiumName}
            fill
            sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
            className="object-cover group-hover:scale-110 transition-transform duration-700 ease-in-out"
          />
        ) : (
          <div className="flex items-center justify-center h-full text-muted-foreground bg-secondary/50">Không có ảnh</div>
        )}

        {/* Overlay Gradients */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/20 to-transparent"></div>

        {/* Distance Badge */}
        {stadium.distanceInKm && (
          <div className="absolute top-4 right-4 bg-white/95 text-gray-900 px-3 py-1.5 rounded-full text-xs font-bold shadow-lg flex items-center backdrop-blur-md">
            <span className="mr-1.5 text-base">🛵</span> {stadium.distanceInKm.toFixed(1)} km
          </div>
        )}

        {/* Bottom Info on Image */}
        <div className="absolute bottom-4 left-4 right-4 text-white">
          <div className="flex items-center text-sm font-medium mb-1.5 bg-black/40 w-fit px-2 py-0.5 rounded-full backdrop-blur-sm">
            <Star className="h-3.5 w-3.5 text-yellow-400 fill-yellow-400 mr-1" />
            <span className="font-bold">{stadium.averageRating}</span>
            <span className="text-gray-300 ml-1.5 text-xs font-normal">({mockReviewCount} đánh giá)</span>
          </div>
          <h3 className="text-xl font-bold truncate text-white drop-shadow-md">{stadium.stadiumName}</h3>
        </div>
      </div>

      <CardContent className="p-5 flex-1 flex flex-col">
        <p className="text-sm text-muted-foreground flex items-start gap-2 mb-4">
          <MapPin className="h-4 w-4 shrink-0 mt-0.5 text-gray-400" />
          <span className="line-clamp-2 leading-relaxed">{stadium.address}</span>
        </p>

        {isUrgent && (
          <div className="mb-4 inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-bold bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 border border-red-100 dark:border-red-900/30 animate-pulse">
            <Flame className="h-4 w-4 mr-1.5" />
            Chỉ còn 1 sân trống giờ này!
          </div>
        )}

        {/* Amenities */}
        <div className="flex flex-wrap gap-2 mt-auto pt-2">
          {stadium.amenities.slice(0, 3).map(a => (
            <span key={a.amenityId} className="text-[10px] bg-gray-50 dark:bg-secondary text-gray-600 dark:text-secondary-foreground px-2.5 py-1 rounded-full font-semibold border border-gray-100 dark:border-border uppercase tracking-wider">
              {a.name}
            </span>
          ))}
          {stadium.amenities.length > 3 && (
            <span className="text-[10px] bg-gray-50 dark:bg-secondary text-gray-600 dark:text-secondary-foreground px-2.5 py-1 rounded-full font-semibold border border-gray-100 dark:border-border uppercase tracking-wider">
              +{stadium.amenities.length - 3}
            </span>
          )}
        </div>
      </CardContent>

      <CardFooter className="p-5 border-t border-gray-100 dark:border-border bg-white dark:bg-card flex justify-between items-center gap-4">
        <div>
          <div className="text-[10px] text-gray-400 font-bold uppercase tracking-wider mb-1">Giá mỗi giờ</div>
          <div className="font-extrabold text-2xl text-gray-900 dark:text-white">
            {stadium.pricePerHour.toLocaleString('vi-VN')}₫
          </div>
        </div>
        <Button asChild className="rounded-xl px-6 py-6 font-bold bg-primary hover:bg-primary/90 text-white shadow-lg shadow-primary/20 transition-all hover:scale-105 active:scale-95">
          <Link href={`/booking/new?stadiumId=${stadium.stadiumId}`}>
            Đặt Ngay
          </Link>
        </Button>
      </CardFooter>
    </Card>
  )
}
