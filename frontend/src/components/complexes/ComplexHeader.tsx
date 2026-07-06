'use client'

import { useState } from 'react'
import Image from 'next/image'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  IconMapPin,
  IconPhone,
  IconStar,
  IconPhoto,
  IconChevronLeft,
  IconChevronRight,
  IconX,
  IconMaximize,
} from '@tabler/icons-react'
import type { StadiumComplexDto, SportTypeDto } from '@/types/complex'

interface ComplexHeaderProps {
  complex: StadiumComplexDto
}

const STATUS_MAP = {
  AVAILABLE: { label: 'Đang hoạt động', cls: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
  MAINTENANCE: { label: 'Đang bảo trì', cls: 'bg-amber-100 text-amber-700 border-amber-200' },
  CLOSED: { label: 'Đã đóng cửa', cls: 'bg-red-100 text-red-700 border-red-200' },
}

const SPORT_ICONS: Record<string, string> = {
  'Bóng đá': '⚽',
  'Cầu lông': '🏸',
  'Bóng rổ': '🏀',
  'Bóng chuyền': '🏐',
  'Tennis': '🎾',
  'Bơi lội': '🏊',
  'Pickle Ball': '🏓',
}

export default function ComplexHeader({ complex }: ComplexHeaderProps) {
  const [lightboxOpen, setLightboxOpen] = useState(false)
  const [lightboxIdx, setLightboxIdx] = useState(0)

  const allImages = complex.images?.map((i) => i.imageUrl) ?? []
  if (complex.coverImageUrl && !allImages.includes(complex.coverImageUrl)) {
    allImages.unshift(complex.coverImageUrl)
  }
  const displayImages = allImages.length > 0 ? allImages : ['/placeholder-venue.jpg']

  const status = STATUS_MAP[complex.complexStatus] ?? STATUS_MAP.AVAILABLE
  const rating = complex.averageRating ?? 0

  const openLightbox = (idx: number) => {
    setLightboxIdx(idx)
    setLightboxOpen(true)
  }
  const closeLightbox = () => setLightboxOpen(false)
  const prevImage = () => setLightboxIdx((i) => (i - 1 + displayImages.length) % displayImages.length)
  const nextImage = () => setLightboxIdx((i) => (i + 1) % displayImages.length)

  return (
    <div className="w-full">
      {/* Image Gallery — mosaic layout */}
      <div className="relative w-full h-[380px] md:h-[460px] bg-gray-100 overflow-hidden">
        <div className="grid h-full grid-cols-4 grid-rows-2 gap-1">
          {/* Main image */}
          <div
            className="col-span-2 row-span-2 relative cursor-pointer group"
            onClick={() => openLightbox(0)}
          >
            <img
              src={displayImages[0]}
              alt={complex.name}
              className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
            />
            <div className="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors" />
          </div>

          {/* Side thumbnails (up to 4) */}
          {[1, 2, 3, 4].map((i) => (
            <div
              key={i}
              className="relative cursor-pointer group overflow-hidden"
              onClick={() => openLightbox(i)}
            >
              {displayImages[i] ? (
                <>
                  <img
                    src={displayImages[i]}
                    alt={`${complex.name} ${i + 1}`}
                    className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                  />
                  {/* "Show all" overlay on last visible thumbnail */}
                  {i === 4 && displayImages.length > 5 && (
                    <div
                      className="absolute inset-0 bg-black/50 flex flex-col items-center justify-center text-white"
                      onClick={(e) => { e.stopPropagation(); openLightbox(4) }}
                    >
                      <IconPhoto className="w-6 h-6 mb-1" />
                      <span className="text-sm font-semibold">+{displayImages.length - 5} ảnh</span>
                    </div>
                  )}
                </>
              ) : (
                <div className="w-full h-full bg-gray-200" />
              )}
            </div>
          ))}
        </div>

        {/* View all photos button */}
        <button
          onClick={() => openLightbox(0)}
          className="absolute bottom-4 right-4 flex items-center gap-2 bg-white/90 backdrop-blur-sm text-gray-800 text-sm font-semibold px-3 py-2 rounded-lg shadow hover:bg-white transition-all"
        >
          <IconMaximize className="w-4 h-4" />
          Xem tất cả
        </button>
      </div>

      {/* Info section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
          {/* Left: Name + Meta */}
          <div className="flex-1 min-w-0">
            {/* Status + Sports badges */}
            <div className="flex flex-wrap items-center gap-2 mb-3">
              <Badge className={`text-xs px-2 py-0.5 border font-medium ${status.cls}`}>
                {status.label}
              </Badge>
              {complex.sportTypes?.map((s: SportTypeDto) => (
                <Badge key={s.sportTypeId} variant="outline" className="text-xs px-2 py-0.5 bg-white">
                  {SPORT_ICONS[s.sportName] ?? '🏟️'} {s.sportName}
                </Badge>
              ))}
            </div>

            <h1 className="text-2xl md:text-3xl font-bold text-gray-900 leading-tight mb-2">
              {complex.name}
            </h1>

            <div className="flex items-center gap-1 text-sm text-gray-500 mb-3">
              <IconMapPin className="w-4 h-4 text-gray-400 flex-shrink-0" />
              <span className="line-clamp-1">{complex.address}</span>
            </div>

            {complex.phone && (
              <div className="flex items-center gap-1 text-sm text-gray-500 mb-3">
                <IconPhone className="w-4 h-4 text-gray-400 flex-shrink-0" />
                <a href={`tel:${complex.phone}`} className="hover:text-emerald-600 transition-colors">
                  {complex.phone}
                </a>
              </div>
            )}

            {complex.description && (
              <p className="text-sm text-gray-600 leading-relaxed line-clamp-3 mt-2">
                {complex.description}
              </p>
            )}
          </div>

          {/* Right: Rating card */}
          <div className="flex-shrink-0">
            <div className="bg-white border border-gray-100 rounded-2xl shadow-sm p-5 min-w-[180px] text-center">
              <div className="text-4xl font-extrabold text-gray-900 leading-none">
                {rating.toFixed(1)}
              </div>
              <div className="flex items-center justify-center gap-0.5 mt-2">
                {[1, 2, 3, 4, 5].map((star) => (
                  <IconStar
                    key={star}
                    className={`w-4 h-4 ${star <= Math.round(rating) ? 'text-amber-400 fill-amber-400' : 'text-gray-200'}`}
                  />
                ))}
              </div>
              <p className="text-xs text-gray-400 mt-1">
                {complex.reviewCount ?? 0} đánh giá
              </p>
              {complex.ownerName && (
                <div className="mt-3 pt-3 border-t border-gray-100">
                  <p className="text-xs text-gray-400">Chủ sân</p>
                  <p className="text-sm font-semibold text-gray-700 mt-0.5">{complex.ownerName}</p>
                  {complex.ownerPhone && (
                    <a
                      href={`tel:${complex.ownerPhone}`}
                      className="text-xs text-emerald-600 hover:underline"
                    >
                      {complex.ownerPhone}
                    </a>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Lightbox */}
      {lightboxOpen && (
        <div
          className="fixed inset-0 z-50 bg-black/95 flex items-center justify-center"
          onClick={closeLightbox}
        >
          <button
            className="absolute top-4 right-4 text-white/80 hover:text-white p-2 rounded-full hover:bg-white/10 transition"
            onClick={closeLightbox}
          >
            <IconX className="w-6 h-6" />
          </button>

          <button
            className="absolute left-4 top-1/2 -translate-y-1/2 text-white/80 hover:text-white p-2 rounded-full hover:bg-white/10 transition"
            onClick={(e) => { e.stopPropagation(); prevImage() }}
          >
            <IconChevronLeft className="w-8 h-8" />
          </button>

          <div
            className="relative w-full max-w-4xl h-[80vh] px-16"
            onClick={(e) => e.stopPropagation()}
          >
            <img
              src={displayImages[lightboxIdx]}
              alt={`${complex.name} photo ${lightboxIdx + 1}`}
              className="w-full h-full object-contain"
            />
          </div>

          <button
            className="absolute right-4 top-1/2 -translate-y-1/2 text-white/80 hover:text-white p-2 rounded-full hover:bg-white/10 transition"
            onClick={(e) => { e.stopPropagation(); nextImage() }}
          >
            <IconChevronRight className="w-8 h-8" />
          </button>

          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 text-white/60 text-sm">
            {lightboxIdx + 1} / {displayImages.length}
          </div>
        </div>
      )}
    </div>
  )
}
