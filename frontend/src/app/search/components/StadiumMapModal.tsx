'use client'

import { useEffect } from 'react'
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet'
import L from 'leaflet'
import { X, Star, MapPin } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { StadiumResponse } from '@/lib/api/stadium'
import 'leaflet/dist/leaflet.css'

// Custom Leaflet Pin Icon using Tailwind
const customIcon = typeof window !== 'undefined' ? L.divIcon({
  className: 'custom-leaflet-marker',
  html: `<div class="w-10 h-10 bg-primary text-white rounded-full border-2 border-white flex items-center justify-center shadow-xl transform transition-all duration-200 hover:scale-115 hover:bg-primary/90">
           <span class="text-lg">⚽</span>
         </div>`,
  iconSize: [40, 40],
  iconAnchor: [20, 40],
  popupAnchor: [0, -40],
}) : undefined

interface StadiumMapModalProps {
  isOpen: boolean
  onClose: () => void
  stadiums: StadiumResponse[]
}

export default function StadiumMapModal({ isOpen, onClose, stadiums }: StadiumMapModalProps) {
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => {
      document.body.style.overflow = ''
    }
  }, [isOpen])

  if (!isOpen) return null

  // Filter stadiums with coordinates
  const validStadiums = stadiums.filter(
    (s) => s.latitude !== undefined && s.latitude !== null && s.longitude !== undefined && s.longitude !== null
  )

  // Calculate Map Center (average coords, default to HCMC center)
  const mapCenter: [number, number] = validStadiums.length > 0
    ? [
        validStadiums.reduce((sum, s) => sum + (s.latitude || 0), 0) / validStadiums.length,
        validStadiums.reduce((sum, s) => sum + (s.longitude || 0), 0) / validStadiums.length,
      ]
    : [10.7769, 106.7009]

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      onClose()
    }
  }

  return (
    <div
      className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm transition-opacity duration-300"
      onClick={handleBackdropClick}
    >
      <div className="relative w-full max-w-4xl bg-white dark:bg-card rounded-3xl overflow-hidden shadow-2xl border border-gray-100 dark:border-border flex flex-col max-h-[90vh]">
        {/* Absolute Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 z-[2000] p-2 bg-white dark:bg-muted text-gray-500 hover:text-gray-900 dark:hover:text-white rounded-full shadow-md border border-gray-100 dark:border-border transition-all hover:scale-105"
          aria-label="Close Map"
        >
          <X className="h-5 w-5" />
        </button>

        {/* Modal Header */}
        <div className="p-6 border-b border-gray-100 dark:border-border flex flex-col pr-16 bg-white dark:bg-card">
          <h3 className="text-xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2">
            📍 Bản đồ Sân Thể Thao
          </h3>
          <p className="text-xs text-muted-foreground mt-1">
            Hiển thị vị trí của {validStadiums.length} sân bóng có định vị GPS
          </p>
        </div>

        {/* Map Container */}
        <div className="w-full relative bg-muted z-10" style={{ height: '500px' }}>
          <MapContainer
            center={mapCenter}
            zoom={13}
            scrollWheelZoom={true}
            style={{ height: '100%', width: '100%' }}
          >
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            {validStadiums.map((stadium) => (
              <Marker
                key={stadium.stadiumId}
                position={[stadium.latitude!, stadium.longitude!]}
                icon={customIcon}
              >
                <Popup className="custom-popup">
                  <div className="w-60 overflow-hidden font-sans p-1">
                    {stadium.firstImageUrl ? (
                      <img
                        src={stadium.firstImageUrl}
                        alt={stadium.stadiumName}
                        className="w-full h-28 object-cover rounded-lg mb-2 shadow-sm"
                      />
                    ) : (
                      <div className="w-full h-28 bg-gray-100 dark:bg-secondary/40 rounded-lg flex items-center justify-center text-xs text-gray-400 mb-2">
                        Không có ảnh
                      </div>
                    )}
                    <h4 className="font-extrabold text-sm text-gray-950 dark:text-white mb-1 truncate">
                      {stadium.stadiumName}
                    </h4>
                    <div className="flex items-center gap-1 mb-1.5">
                      <Star className="w-3.5 h-3.5 text-yellow-400 fill-yellow-400" />
                      <span className="text-xs font-bold text-gray-800 dark:text-gray-200">
                        {stadium.averageRating}
                      </span>
                    </div>
                    <p className="text-[11px] text-gray-500 dark:text-gray-400 flex gap-1 items-start mb-3">
                      <MapPin className="w-3 h-3 shrink-0 text-gray-400 mt-0.5" />
                      <span className="line-clamp-2 leading-relaxed">{stadium.address}</span>
                    </p>
                    <div className="flex justify-between items-center pt-2 border-t border-gray-100 dark:border-border">
                      <div>
                        <span className="text-[8px] uppercase tracking-wider text-gray-400 block font-bold">
                          Giá mỗi giờ
                        </span>
                        <span className="font-extrabold text-sm text-gray-900 dark:text-white">
                          {stadium.pricePerHour.toLocaleString('vi-VN')}₫
                        </span>
                      </div>
                      <Button
                        size="sm"
                        className="bg-primary hover:bg-primary/90 text-white font-bold rounded-lg text-[11px] px-3 py-1.5 shadow-md shadow-primary/10 transition-all hover:scale-105 active:scale-95"
                      >
                        Đặt sân
                      </Button>
                    </div>
                  </div>
                </Popup>
              </Marker>
            ))}
          </MapContainer>
        </div>
      </div>
    </div>
  )
}
