'use client'

import { useEffect, useMemo, useState } from 'react'
import type { ComponentProps } from 'react'
import { Circle, MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet'
import L from 'leaflet'
import { Star, MapPin } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { StadiumComplexDto } from '@/types/complex'
import Image from 'next/image'
import Link from 'next/link'
import { useSearchParams } from 'next/navigation'
import 'leaflet/dist/leaflet.css'

const getUnifiedIcon = (isHovered: boolean = false): L.DivIcon | undefined => {
  if (typeof window === 'undefined') return undefined;

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

const NEARBY_MARKER_THRESHOLD_METERS = 500
const MARKER_OFFSET_RADIUS_PX = 28

const distanceInMeters = (a: StadiumComplexDto, b: StadiumComplexDto): number => {
  if (a.latitude == null || a.longitude == null || b.latitude == null || b.longitude == null) return Infinity
  return L.latLng(a.latitude, a.longitude).distanceTo(L.latLng(b.latitude, b.longitude))
}

type SearchCircle = {
  center: [number, number]
  radiusMeters: number
}

// Toạ độ trung tâm gần đúng của Việt Nam + zoom đủ thấy toàn bộ đất nước —
// dùng khi chưa chọn tỉnh/thành (thay vì zoom cố định 12 theo điểm trung bình
// toạ độ nationwide, dễ gây hiểu lầm "chỉ có vài sân" khi điểm trung bình rơi
// vào 1 khu vực hẹp trong khi kết quả trải khắp cả nước).
const VIETNAM_CENTER: [number, number] = [16.0, 106.0]
const VIETNAM_ZOOM = 5

function getOffsetForCluster(index: number, size: number): L.PointExpression {
  if (size <= 1) return [0, 0]
  const angle = (Math.PI * 2 * index) / size - Math.PI / 2
  return [
    Math.round(Math.cos(angle) * MARKER_OFFSET_RADIUS_PX),
    Math.round(Math.sin(angle) * MARKER_OFFSET_RADIUS_PX),
  ]
}

function OffsetMarker({
  position,
  pixelOffset,
  children,
  ...markerProps
}: ComponentProps<typeof Marker> & {
  position: [number, number]
  pixelOffset: L.PointExpression
}) {
  const map = useMap()
  const [zoom, setZoom] = useState(map.getZoom())

  useEffect(() => {
    const updateZoom = () => setZoom(map.getZoom())
    map.on('zoomend', updateZoom)
    return () => {
      map.off('zoomend', updateZoom)
    }
  }, [map])

  const markerPosition = useMemo(() => {
    const point = map.project(position, zoom)
    const offsetPoint = L.point(point).add(L.point(pixelOffset))
    const latLng = map.unproject(offsetPoint, zoom)
    return [latLng.lat, latLng.lng] as [number, number]
  }, [map, pixelOffset, position, zoom])

  return (
    <Marker position={markerPosition} {...markerProps}>
      {children}
    </Marker>
  )
}

// Component to recenter map when complexes or nearby search radius change.
// isNationwide forces the wide Vietnam-wide zoom every time (so clearing the
// province filter always shows full-country context); otherwise preserves
// whatever zoom the user currently has (unchanged from before — filter
// changes shouldn't undo a manual zoom).
function MapViewportHandler({ center, isNationwide, searchCircle }: { center: [number, number], isNationwide: boolean, searchCircle: SearchCircle | null }) {
  const map = useMap()
  useEffect(() => {
    if (!map) return
    if (searchCircle) {
      const tempCircle = L.circle(searchCircle.center, { radius: searchCircle.radiusMeters }).addTo(map)
      const circleBounds = tempCircle.getBounds()
      tempCircle.remove()
      map.fitBounds(circleBounds, { padding: [32, 32], animate: false })
    } else {
      map.setView(center, isNationwide ? VIETNAM_ZOOM : map.getZoom(), { animate: false })
    }
  }, [center, isNationwide, map, searchCircle])
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
  userLat?: number
  userLng?: number
  radiusInKm?: number
  province?: string
}

export default function ComplexMap({ complexes, hoveredComplexId, userLat, userLng, radiusInKm, province }: ComplexMapProps) {
  const isNationwide = !province
  const searchParams = useSearchParams()
  const sportTypeId = searchParams.get('sportTypeId')
  const detailHref = (complexId: number) =>
    sportTypeId
      ? `/complexes/${complexId}?sportTypeId=${sportTypeId}&tab=courts`
      : `/complexes/${complexId}?tab=courts`

  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  const validComplexes = complexes.filter(
    (c) => c.latitude !== undefined && c.latitude !== null && c.longitude !== undefined && c.longitude !== null
  )

  const markerOffsetsByComplexId = useMemo(() => {
    const clusters: StadiumComplexDto[][] = []

    validComplexes.forEach((complex) => {
      const cluster = clusters.find((items) =>
        items.some((item) => distanceInMeters(item, complex) <= NEARBY_MARKER_THRESHOLD_METERS)
      )
      if (cluster) {
        cluster.push(complex)
      } else {
        clusters.push([complex])
      }
    })

    const offsets = new Map<number, L.PointExpression>()
    clusters.forEach((cluster) => {
      cluster
        .sort((a, b) => a.complexId - b.complexId)
        .forEach((complex, index) => {
          offsets.set(complex.complexId, getOffsetForCluster(index, cluster.length))
        })
    })
    return offsets
  }, [validComplexes])

  const hoveredComplex = hoveredComplexId ? validComplexes.find(c => c.complexId === hoveredComplexId) : null;
  const hoveredLat = hoveredComplex?.latitude
  const hoveredLng = hoveredComplex?.longitude
  const hoveredCenter: [number, number] | null = useMemo(
    () => (hoveredLat && hoveredLng ? [hoveredLat, hoveredLng] : null),
    [hoveredLat, hoveredLng]
  )

  // Memoized on `complexes` (not on hover state) so hovering/unhovering a card
  // doesn't re-trigger MapRecenter and snap the map back to the average center.
  // Khi chưa chọn tỉnh/thành (isNationwide), luôn dùng tâm Việt Nam thay vì
  // điểm trung bình toạ độ nationwide — tránh zoom vào 1 khu vực hẹp trong khi
  // kết quả trải khắp cả nước (xem VIETNAM_CENTER/VIETNAM_ZOOM ở trên).
  const mapCenter: [number, number] = useMemo(() => {
    if (isNationwide) return VIETNAM_CENTER
    return validComplexes.length > 0
      ? [
          validComplexes.reduce((sum, c) => sum + (c.latitude || 0), 0) / validComplexes.length,
          validComplexes.reduce((sum, c) => sum + (c.longitude || 0), 0) / validComplexes.length,
        ]
      : [10.7769, 106.7009]
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [complexes, isNationwide])

  const searchCircle: SearchCircle | null = useMemo(() => {
    if (
      userLat === undefined || userLng === undefined || radiusInKm === undefined
      || Number.isNaN(userLat) || Number.isNaN(userLng) || Number.isNaN(radiusInKm)
    ) {
      return null
    }
    return {
      center: [userLat, userLng],
      radiusMeters: radiusInKm * 1000,
    }
  }, [radiusInKm, userLat, userLng])

  if (!mounted) return <div className="w-full h-full bg-muted animate-pulse rounded-2xl" />

  return (
    <div className="w-full h-full relative bg-muted rounded-2xl overflow-hidden z-10 border border-gray-100 shadow-inner">
      {isNationwide && (
        <div className="absolute top-3 left-1/2 -translate-x-1/2 z-[1000] bg-white/90 dark:bg-gray-900/90 backdrop-blur-sm text-[11px] font-medium text-gray-600 dark:text-gray-300 px-3 py-1.5 rounded-full shadow-md border border-gray-100 dark:border-border pointer-events-none">
          Đang hiện toàn quốc — chọn tỉnh/thành để xem chi tiết khu vực
        </div>
      )}
      <MapContainer
        center={mapCenter}
        zoom={isNationwide ? VIETNAM_ZOOM : 12}
        scrollWheelZoom={true}
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <MapViewportHandler center={mapCenter} isNationwide={isNationwide} searchCircle={searchCircle} />
        <MapFlyToHandler center={hoveredCenter} />
        {searchCircle && (
          <Circle
            center={searchCircle.center}
            radius={searchCircle.radiusMeters}
            pathOptions={{
              color: '#059669',
              fillColor: '#10b981',
              fillOpacity: 0.12,
              opacity: 0.65,
              weight: 2,
            }}
          />
        )}
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
            <OffsetMarker
              key={complex.complexId}
              position={[complex.latitude!, complex.longitude!]}
              pixelOffset={markerOffsetsByComplexId.get(complex.complexId) || [0, 0]}
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
                      {complex.reviewCount && complex.reviewCount > 0 ? (complex.averageRating || 5.0).toFixed(1) : '—'}
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
                      <Link href={detailHref(complex.complexId)}>Chi tiết</Link>
                    </Button>
                  </div>
                </div>
              </Popup>
            </OffsetMarker>
          )
        })}
      </MapContainer>
    </div>
  )
}
