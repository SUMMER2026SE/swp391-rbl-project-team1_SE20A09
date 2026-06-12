'use client'

import { useState, useCallback, useEffect, useRef } from 'react'
import dynamic from 'next/dynamic'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card } from '@/components/ui/card'
import { MapPin } from 'lucide-react'
import 'leaflet/dist/leaflet.css'

const DA_NANG_CENTER = { lat: 16.0544, lng: 108.2022 }

interface AddressData {
  addressText: string
  lat: number
  lng: number
}

interface Props {
  initialAddress?: string
  initialLat?: number
  initialLng?: number
  onAddressChange: (data: AddressData) => void
}

interface LocationDTO {
  displayName: string
  latitude: number
  longitude: number
  province?: string
  district?: string
  ward?: string
  street?: string
  source?: string
}

const LeafletMap = dynamic(
  () => import('./LeafletMap'),
  {
    ssr: false,
    loading: () => <div className="h-full w-full flex items-center justify-center bg-muted">Đang tải bản đồ...</div>
  }
)

export function AddressPicker({ initialAddress, initialLat, initialLng, onAddressChange }: Props) {
  const [position, setPosition] = useState<{ lat: number; lng: number }>(() => {
    if (initialLat && initialLng) return { lat: initialLat, lng: initialLng }
    return DA_NANG_CENTER
  })
  const [inputValue, setInputValue] = useState(initialAddress || '')
  const [suggestions, setSuggestions] = useState<LocationDTO[]>([])
  const [showDropdown, setShowDropdown] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const debounceTimerRef = useRef<NodeJS.Timeout>()
  const wrapperRef = useRef<HTMLDivElement>(null)
  const [isClient, setIsClient] = useState(false)

  // Memory cache
  const cacheRef = useRef<Map<string, LocationDTO[]>>(new Map())
  const reverseCacheRef = useRef<Map<string, LocationDTO>>(new Map())

  useEffect(() => {
    setIsClient(true)
    if (typeof window !== 'undefined') {
      const L = require('leaflet')
      delete (L.Icon.Default.prototype as any)._getIconUrl
      L.Icon.Default.mergeOptions({
        iconRetinaUrl: '/leaflet/marker-icon-2x.png',
        iconUrl: '/leaflet/marker-icon.png',
        shadowUrl: '/leaflet/marker-shadow.png',
      })
    }
  }, [])

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target as Node)) {
        setShowDropdown(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const reverseGeocode = useCallback(
    async (lat: number, lng: number) => {
      const cacheKey = `${lat},${lng}`
      if (reverseCacheRef.current.has(cacheKey)) {
        const cached = reverseCacheRef.current.get(cacheKey)!
        setInputValue(cached.displayName)
        onAddressChange({ addressText: cached.displayName, lat, lng })
        return
      }

      try {
        const res = await fetch(`/api/v1/geocoding/reverse?lat=${lat}&lng=${lng}`)
        if (!res.ok) throw new Error('Reverse fetch failed')
        const data: LocationDTO = await res.json()
        
        if (data && data.displayName) {
          reverseCacheRef.current.set(cacheKey, data)
          setInputValue(data.displayName)
          onAddressChange({ addressText: data.displayName, lat, lng })
        } else {
          setInputValue('')
          onAddressChange({ addressText: '', lat, lng })
        }
      } catch (err) {
        console.error('Reverse geocode error:', err)
        setInputValue('')
        onAddressChange({ addressText: '', lat, lng })
      }
    },
    [onAddressChange]
  )

  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value
    setInputValue(value)

    if (value.trim().length < 3) {
      setSuggestions([])
      setShowDropdown(false)
      setIsLoading(false)
      return
    }

    if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current)

    debounceTimerRef.current = setTimeout(async () => {
      const q = value.trim()
      if (cacheRef.current.has(q)) {
        setSuggestions(cacheRef.current.get(q)!)
        setShowDropdown(true)
        return
      }

      setIsLoading(true)
      try {
        const res = await fetch(`/api/v1/geocoding/search?q=${encodeURIComponent(q)}`)
        if (!res.ok) throw new Error('Search failed')
        const data: LocationDTO[] = await res.json()
        cacheRef.current.set(q, data)
        setSuggestions(data)
        setShowDropdown(true)
      } catch (err) {
        console.error('Autocomplete error:', err)
        setSuggestions([])
        setShowDropdown(true)
      } finally {
        setIsLoading(false)
      }
    }, 500)
  }, [])

  const selectSuggestion = useCallback(
    (item: LocationDTO) => {
      const lat = item.latitude
      const lng = item.longitude
      setPosition({ lat, lng })
      setInputValue(item.displayName)
      setShowDropdown(false)
      onAddressChange({ addressText: item.displayName, lat, lng })
    },
    [onAddressChange]
  )

  const handleMarkerDragEnd = useCallback(
    (lat: number, lng: number) => {
      setPosition({ lat, lng })
      setInputValue('Đang tải địa chỉ...')
      reverseGeocode(lat, lng)
    },
    [reverseGeocode]
  )

  const handleMapClick = useCallback(
    (lat: number, lng: number) => {
      setPosition({ lat, lng })
      setInputValue('Đang tải địa chỉ...')
      reverseGeocode(lat, lng)
    },
    [reverseGeocode]
  )

  if (!isClient) {
    return (
      <div className="space-y-4">
        <div className="relative">
          <Label htmlFor="address-input">Địa chỉ sân *</Label>
          <div className="relative mt-2">
            <MapPin className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
            <Input id="address-input" placeholder="Đang tải bản đồ..." className="pl-10" disabled />
          </div>
        </div>
        <div className="h-[400px] rounded-lg overflow-hidden border bg-muted flex items-center justify-center">
          Đang tải bản đồ...
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <div className="relative" ref={wrapperRef}>
        <Label htmlFor="address-input">Địa chỉ sân *</Label>
        <div className="relative mt-2">
          <MapPin className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
          <Input
            id="address-input"
            value={inputValue}
            onChange={handleInputChange}
            onFocus={() => {
              if (inputValue.length >= 3) setShowDropdown(true)
            }}
            placeholder={inputValue === '' ? 'Nhập tay địa chỉ (Không tìm thấy tự động)' : 'Nhập địa chỉ (VD: 45 Lê Lợi, Quận 1...)'}
            className="pl-10"
            autoComplete="off"
          />
          {isLoading && (
            <div className="absolute right-3 top-3 text-xs text-muted-foreground">
              Đang tìm...
            </div>
          )}
        </div>
        {showDropdown && (
          <Card className="absolute z-50 w-full mt-1 max-h-60 overflow-y-auto">
            {suggestions.length > 0 ? (
              suggestions.map((item, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => selectSuggestion(item)}
                  className="w-full text-left px-4 py-3 hover:bg-muted transition-colors text-sm border-b last:border-b-0"
                >
                  {item.displayName}
                </button>
              ))
            ) : (
              <div className="px-4 py-3 text-sm text-muted-foreground">
                Không tìm thấy địa chỉ phù hợp.
              </div>
            )}
          </Card>
        )}
      </div>
      <p className="text-sm text-muted-foreground mt-1">Hoặc ghim vị trí trực tiếp trên bản đồ bên dưới</p>

      <div className="h-[400px] rounded-lg overflow-hidden border mt-2">
        <LeafletMap position={position} onMapClick={handleMapClick} onDragEnd={handleMarkerDragEnd} />
      </div>
    </div>
  )
}
