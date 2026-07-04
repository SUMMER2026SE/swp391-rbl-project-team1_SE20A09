'use client'

import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet'
import L from 'leaflet'
import { Star, MapPin } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { StadiumComplexDto } from '@/types/complex'
import Image from 'next/image'
import Link from 'next/link'
import 'leaflet/dist/leaflet.css'

const getSportEmoji = (sportName?: string) => {
  switch (sportName?.toLowerCase()) {
    case 'football': return '⚽';
    case 'badminton': return '🏸';
    case 'basketball': return '🏀';
    case 'tennis': return '🎾';
    case 'volleyball': return '🏐';
    default: return '🏢';
  }
}

const getCustomIcon = (sportName?: string) => {
  if (typeof window === 'undefined') return undefined;
  return L.divIcon({
    className: 'custom-leaflet-marker',
    html: `<div class="w-10 h-10 bg-primary text-white rounded-full border-2 border-white flex items-center justify-center shadow-xl transform transition-all duration-200 hover:scale-110 hover:bg-primary/90">
             <span class="text-lg">${getSportEmoji(sportName)}</span>
           </div>`,
    iconSize: [40, 40],
    iconAnchor: [20, 40],
    popupAnchor: [0, -40],
  })
}

// Component to recenter map when complexes change
function MapRecenter({ center }: { center: [number, number] }) {
  const map = useMap()
  useEffect(() => {
    map.setView(center)
  }, [center, map])
  return null
}

interface ComplexMapProps {
  complexes: StadiumComplexDto[]
}

export default function ComplexMap({ complexes }: ComplexMapProps) {
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  if (!mounted) return <div className="w-full h-full bg-muted animate-pulse rounded-2xl" />

  const validComplexes = complexes.filter(
    (c) => c.latitude !== undefined && c.latitude !== null && c.longitude !== undefined && c.longitude !== null
  )

  const mapCenter: [number, number] = validComplexes.length > 0
    ? [
        validComplexes.reduce((sum, c) => sum + (c.latitude || 0), 0) / validComplexes.length,
        validComplexes.reduce((sum, c) => sum + (c.longitude || 0), 0) / validComplexes.length,
      ]
    : [10.7769, 106.7009]

  return (
    <div className="w-full h-full relative bg-muted rounded-2xl overflow-hidden z-10 border border-gray-100 shadow-inner">
      <MapContainer
        center={mapCenter}
        zoom={12}
        scrollWheelZoom={true}
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <MapRecenter center={mapCenter} />
        {validComplexes.map((complex) => {
          const primarySport = complex.sportTypes?.[0]?.sportName
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
            <Marker
              key={complex.complexId}
              position={[complex.latitude!, complex.longitude!]}
              icon={getCustomIcon(primarySport)}
            >
              <Popup className="custom-popup">
                <div className="w-60 overflow-hidden font-sans p-1">
                  {complex.coverImageUrl ? (
                    <div className="relative w-full h-28 mb-2">
                      <Image
                        src={complex.coverImageUrl}
                        alt={complex.name}
                        fill
                        unoptimized
                        priority
                        className="object-cover rounded-lg shadow-sm"
                      />
                    </div>
                  ) : (
                    <div className="w-full h-28 bg-gray-100 dark:bg-secondary/40 rounded-lg flex items-center justify-center text-xs text-gray-400 mb-2">
                      Không có ảnh
                    </div>
                  )}
                  <h4 className="font-extrabold text-sm text-gray-950 dark:text-white mb-1 truncate">
                    {complex.name}
                  </h4>
                  <div className="flex items-center gap-1 mb-1.5">
                    <Star className="w-3.5 h-3.5 text-yellow-400 fill-yellow-400" />
                    <span className="text-xs font-bold text-gray-800 dark:text-gray-200">
                      {complex.totalReviews && complex.totalReviews > 0 ? (complex.averageRating || 5.0).toFixed(1) : '—'}
                    </span>
                  </div>
                  <p className="text-[11px] text-gray-500 dark:text-gray-400 flex gap-1 items-start mb-3">
                    <MapPin className="w-3 h-3 shrink-0 text-gray-400 mt-0.5" />
                    <span className="line-clamp-2 leading-relaxed">{complex.address}</span>
                  </p>
                  <div className="flex justify-between items-center pt-2 border-t border-gray-100 dark:border-border">
                    <div className="max-w-[120px]">
                      <span className="text-[8px] uppercase tracking-wider text-gray-400 block font-bold">
                        Giá thuê
                      </span>
                      <span className="font-extrabold text-xs text-gray-900 dark:text-white truncate block">
                        {formattedPrice}
                      </span>
                    </div>
                    <Button
                      asChild
                      size="sm"
                      className="bg-primary hover:bg-primary/90 text-white font-bold rounded-lg text-[11px] px-3 py-1.5 h-7 shadow-md shadow-primary/10 transition-all hover:scale-105"
                    >
                      <Link href={`/complexes/${complex.complexId}`}>Chi tiết</Link>
                    </Button>
                  </div>
                </div>
              </Popup>
            </Marker>
          )
        })}
      </MapContainer>
    </div>
  )
}
