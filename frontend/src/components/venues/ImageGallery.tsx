'use client'

import { useState } from 'react'
import { ChevronLeft, ChevronRight, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent } from '@/components/ui/dialog'

interface ImageGalleryProps {
  images: string[]
  venueName: string
}

export default function ImageGallery({ images, venueName }: ImageGalleryProps) {
  const [currentIndex, setCurrentIndex] = useState(0)
  const [lightboxOpen, setLightboxOpen] = useState(false)

  const next = () => setCurrentIndex((i) => (i + 1) % images.length)
  const prev = () => setCurrentIndex((i) => (i - 1 + images.length) % images.length)

  if (images.length === 0) {
    return (
      <div className="relative h-[500px] bg-muted flex items-center justify-center">
        <p className="text-muted-foreground">Không có hình ảnh</p>
      </div>
    )
  }

  return (
    <>
      <div className="relative h-[500px] overflow-hidden group">
        <img
          src={images[currentIndex]}
          alt={`${venueName} - ${currentIndex + 1}`}
          className="w-full h-full object-cover cursor-pointer"
          onClick={() => setLightboxOpen(true)}
        />
        
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />

        {images.length > 1 && (
          <>
            <Button
              variant="ghost"
              size="icon"
              className="absolute left-4 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white opacity-0 group-hover:opacity-100 transition-opacity"
              onClick={prev}
            >
              <ChevronLeft className="h-6 w-6" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="absolute right-4 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white opacity-0 group-hover:opacity-100 transition-opacity"
              onClick={next}
            >
              <ChevronRight className="h-6 w-6" />
            </Button>

            <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2">
              {images.map((_, i) => (
                <button
                  key={i}
                  className={`h-2 rounded-full transition-all ${
                    i === currentIndex ? 'w-8 bg-white' : 'w-2 bg-white/50'
                  }`}
                  onClick={() => setCurrentIndex(i)}
                />
              ))}
            </div>
          </>
        )}

        <div className="absolute bottom-4 right-4 bg-black/70 text-white px-3 py-1 rounded-full text-sm">
          {currentIndex + 1} / {images.length}
        </div>
      </div>

      <Dialog open={lightboxOpen} onOpenChange={setLightboxOpen}>
        <DialogContent className="max-w-7xl p-0 bg-black">
          <div className="relative">
            <img
              src={images[currentIndex]}
              alt={`${venueName} - ${currentIndex + 1}`}
              className="w-full h-auto max-h-[90vh] object-contain"
            />
            <Button
              variant="ghost"
              size="icon"
              className="absolute top-4 right-4 bg-black/50 hover:bg-black/70 text-white"
              onClick={() => setLightboxOpen(false)}
            >
              <X className="h-6 w-6" />
            </Button>
            {images.length > 1 && (
              <>
                <Button
                  variant="ghost"
                  size="icon"
                  className="absolute left-4 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white"
                  onClick={prev}
                >
                  <ChevronLeft className="h-8 w-8" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  className="absolute right-4 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white"
                  onClick={next}
                >
                  <ChevronRight className="h-8 w-8" />
                </Button>
              </>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </>
  )
}
