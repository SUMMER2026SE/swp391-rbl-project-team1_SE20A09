import { Card, CardContent, CardFooter } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { MapPin, Star } from 'lucide-react'
import type { StadiumComplexDto } from '@/types/complex'
import Image from 'next/image'
import Link from 'next/link'

interface ComplexCardProps {
  complex: StadiumComplexDto
}

export function ComplexCard({ complex }: ComplexCardProps) {
  const formattedPrice = (() => {
    if (complex.minPrice !== undefined && complex.minPrice !== null) {
      if (complex.maxPrice !== undefined && complex.maxPrice !== null && complex.minPrice !== complex.maxPrice) {
        return `${Number(complex.minPrice).toLocaleString('vi-VN')}₫ - ${Number(complex.maxPrice).toLocaleString('vi-VN')}₫`
      }
      return `${Number(complex.minPrice).toLocaleString('vi-VN')}₫`
    }
    return 'Chưa cập nhật'
  })()

  return (
    <Card className="overflow-hidden bg-card hover:shadow-2xl transition-all duration-300 border-gray-100 dark:border-border group cursor-pointer flex flex-col h-full rounded-2xl">
      <Link href={`/complexes/${complex.complexId}`} className="flex flex-col flex-1">
        <div className="relative h-64 w-full bg-muted overflow-hidden">
          {complex.coverImageUrl ? (
            <img
              src={complex.coverImageUrl}
              alt={complex.name}
              className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700 ease-in-out"
            />
          ) : (
            <div className="flex items-center justify-center h-full text-muted-foreground bg-secondary/50 font-bold">Không có ảnh</div>
          )}

          {/* Overlay Gradients */}
          <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/20 to-transparent"></div>

          {/* Distance Badge */}
          {complex.distanceInKm !== undefined && complex.distanceInKm !== null && (
            <div className="absolute top-4 right-4 bg-white/95 text-gray-900 px-3 py-1.5 rounded-full text-xs font-bold shadow-lg flex items-center backdrop-blur-md">
              <span className="mr-1.5 text-base">🛵</span> {complex.distanceInKm.toFixed(1)} km
            </div>
          )}

          {/* Bottom Info on Image */}
          <div className="absolute bottom-4 left-4 right-4 text-white">
            <div className="flex items-center text-sm font-medium mb-1.5 bg-black/40 w-fit px-2 py-0.5 rounded-full backdrop-blur-sm">
              <Star className="h-3.5 w-3.5 text-yellow-400 fill-yellow-400 mr-1" />
              <span className="font-bold">
                {complex.reviewCount && complex.reviewCount > 0 ? (complex.averageRating || 5.0).toFixed(1) : '—'}
              </span>
              <span className="text-gray-300 ml-1.5 text-xs font-normal">({complex.reviewCount || 0} đánh giá)</span>
            </div>
            <h3 className="text-xl font-bold truncate text-white drop-shadow-md">{complex.name}</h3>
          </div>
        </div>

        <CardContent className="p-5 flex-1 flex flex-col justify-between">
          <p className="text-sm text-muted-foreground flex items-start gap-2 mb-4">
            <MapPin className="h-4 w-4 shrink-0 mt-0.5 text-gray-400" />
            <span className="line-clamp-2 leading-relaxed">{complex.address}</span>
          </p>

          <div className="space-y-3">
            {/* Sports */}
            {complex.sportTypes && complex.sportTypes.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {complex.sportTypes.map(st => (
                  <span key={st.sportTypeId} className="text-[10px] bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 px-2 py-0.5 rounded font-bold uppercase tracking-wide">
                    ⚽ {st.sportName}
                  </span>
                ))}
              </div>
            )}

            {/* Amenities */}
            {complex.amenities && complex.amenities.length > 0 && (
              <div className="flex flex-wrap gap-1.5 pt-1">
                {complex.amenities.slice(0, 3).map(a => (
                  <span key={a.amenityId} className="text-[10px] bg-gray-50 dark:bg-secondary text-gray-600 dark:text-secondary-foreground px-2.5 py-1 rounded-full font-semibold border border-gray-100 dark:border-border">
                    {a.name}
                  </span>
                ))}
                {complex.amenities.length > 3 && (
                  <span className="text-[10px] bg-gray-50 dark:bg-secondary text-gray-600 dark:text-secondary-foreground px-2.5 py-1 rounded-full font-semibold border border-gray-100 dark:border-border">
                    +{complex.amenities.length - 3}
                  </span>
                )}
              </div>
            )}
          </div>
        </CardContent>
      </Link>

      <CardFooter className="p-5 border-t border-gray-100 dark:border-border bg-white dark:bg-card flex justify-between items-center gap-4">
        <Link href={`/complexes/${complex.complexId}`} className="hover:opacity-80 transition-opacity">
          <div className="text-[10px] text-gray-400 font-bold uppercase tracking-wider mb-1">Giá thuê mỗi giờ</div>
          <div className="font-extrabold text-lg text-gray-900 dark:text-white truncate max-w-[180px]">
            {formattedPrice}
          </div>
        </Link>
        <Button asChild className="rounded-xl px-6 py-6 font-bold bg-primary hover:bg-primary/90 text-white shadow-lg shadow-primary/20 transition-all hover:scale-105 active:scale-95">
          <Link href={`/complexes/${complex.complexId}`}>
            Xem Chi Tiết
          </Link>
        </Button>
      </CardFooter>
    </Card>
  )
}
