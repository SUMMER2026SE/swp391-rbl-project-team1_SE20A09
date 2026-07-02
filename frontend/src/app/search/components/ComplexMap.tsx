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

const getUnifiedIcon = (isHovered: boolean = false) => {
  if (typeof window === 'undefined') return undefined as any;
  
  const bgColor = isHovered ? 'bg-gray-900 border-yellow-400' : 'bg-primary border-white';
  const svgColor = isHovered ? 'text-yellow-400' : 'text-white';
  const transform = isHovered ? 'scale-125 -translate-y-2' : 'hover:scale-110 hover:-translate-y-1';
  const shadow = isHovered ? '-bottom-2 w-7 h-2 bg-black/40' : '-bottom-1.5 w-5 h-1.5 bg-black/30 group-hover:w-6 group-hover:h-2 group-hover:bg-black/20';

  return L.divIcon({
    className: 'bg-transparent border-0',
    html: `
      <div class="relative flex items-center justify-center w-10 h-10 transform transition-all duration-300 ${transform} group cursor-pointer">
        <div class="absolute inset-0 ${bgColor} rounded-full shadow-lg border-[3px] flex items-center justify-center z-10 transition-colors">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="${svgColor} drop-shadow-sm">
            <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"></path>
            <circle cx="12" cy="10" r="3"></circle>
          </svg>
        </div>
        <div class="absolute ${shadow} rounded-[100%] blur-[2px] transition-all duration-300"></div>
      </div>
    `,
    iconSize: [40, 40],
    iconAnchor: [20, 40],
    popupAnchor: [0, -45],
  })
}

// Component to recenter map when complexes change
function MapRecenter({ center }: { center: [number, number] }) {
  const map = useMap()
  useEffect(() => {
    if (map) {
      map.setView(center, map.getZoom(), { animate: false })
    }
  }, [center, map])
  return null
}

// Component to fly to hovered complex
function MapFlyToHandler({ center }: { center: [number, number] | null }) {
  const map = useMap()
  useEffect(() => {
    if (map && center) {
      map.flyTo(center, map.getZoom(), { animate: true, duration: 1.0 })
    }
  }, [center, map])
  return null
}

interface ComplexMapProps {
  complexes: StadiumComplexDto[]
  hoveredComplexId?: number | null
}

export default function ComplexMap({ complexes, hoveredComplexId }: ComplexMapProps) {
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

  const hoveredComplex = hoveredComplexId ? validComplexes.find(c => c.complexId === hoveredComplexId) : null;
  const hoveredCenter: [number, number] | null = hoveredComplex && hoveredComplex.latitude && hoveredComplex.longitude 
    ? [hoveredComplex.latitude, hoveredComplex.longitude] 
    : null;

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
        <MapFlyToHandler center={hoveredCenter} />
        {validComplexes.map((complex) => {
          const formattedPrice = (() => {
            if (complex.minPrice !== undefined && complex.minPrice !== null) {
              if (complex.maxPrice !== undefined && complex.maxPrice !== null && complex.minPrice !== complex.maxPrice) {
                return `${Number(complex.minPrice).toLocaleString('vi-VN')}₫ - ${Number(complex.maxPrice).toLocaleString('vi-VN')}₫`
              }
              return `${Number(complex.minPrice).toLocaleString('vi-VN')}₫`
            }
            return 'Chưa cập nhật'
          })()

          const isHovered = hoveredComplexId === complex.complexId;

          return (
            <Marker
              key={complex.complexId}
              position={[complex.latitude!, complex.longitude!]}
              icon={getUnifiedIcon(isHovered)}
              zIndexOffset={isHovered ? 1000 : 0}
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
