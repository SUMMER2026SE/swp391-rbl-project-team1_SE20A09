'use client'

import { useState, useCallback, useEffect, useRef } from 'react'
import { APIProvider, Map, AdvancedMarker } from '@vis.gl/react-google-maps'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card } from '@/components/ui/card'
import { MapPin } from 'lucide-react'

const VN_CENTER = { lat: 10.8231, lng: 106.6297 }
const API_KEY = process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY || ''

interface AddressData {
  addressText: string
  lat: number
  lng: number
}

interface Props {
  onAddressChange: (data: AddressData) => void
}

export function AddressPicker({ onAddressChange }: Props) {
  const [position, setPosition] = useState(VN_CENTER)
  const [inputValue, setInputValue] = useState('')
  const [predictions, setPredictions] = useState<google.maps.places.AutocompletePrediction[]>([])
  const [showDropdown, setShowDropdown] = useState(false)
  const sessionTokenRef = useRef<google.maps.places.AutocompleteSessionToken | null>(null)
  const autocompleteServiceRef = useRef<google.maps.places.AutocompleteService | null>(null)
  const placesServiceRef = useRef<google.maps.places.PlacesService | null>(null)
  const geocoderRef = useRef<google.maps.Geocoder | null>(null)
  const debounceTimerRef = useRef<NodeJS.Timeout>()

  useEffect(() => {
    if (typeof google !== 'undefined') {
      if (!autocompleteServiceRef.current) {
        autocompleteServiceRef.current = new google.maps.places.AutocompleteService()
      }
      if (!geocoderRef.current) {
        geocoderRef.current = new google.maps.Geocoder()
      }
      if (!sessionTokenRef.current) {
        sessionTokenRef.current = new google.maps.places.AutocompleteSessionToken()
      }
    }
  }, [])

  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value
    setInputValue(value)

    if (!value.trim()) {
      setPredictions([])
      setShowDropdown(false)
      return
    }

    if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current)

    debounceTimerRef.current = setTimeout(() => {
      if (!autocompleteServiceRef.current || !sessionTokenRef.current) return

      autocompleteServiceRef.current.getPlacePredictions(
        {
          input: value,
          sessionToken: sessionTokenRef.current,
          componentRestrictions: { country: 'vn' },
        },
        (results, status) => {
          if (status === google.maps.places.PlacesServiceStatus.OK && results) {
            setPredictions(results)
            setShowDropdown(true)
          } else {
            setPredictions([])
          }
        }
      )
    }, 350)
  }, [])

  const selectPrediction = useCallback((placeId: string, description: string) => {
    if (!placesServiceRef.current) {
      const mapDiv = document.createElement('div')
      placesServiceRef.current = new google.maps.places.PlacesService(mapDiv)
    }

    placesServiceRef.current.getDetails(
      { placeId, fields: ['geometry', 'formatted_address'], sessionToken: sessionTokenRef.current || undefined },
      (place, status) => {
        if (status === google.maps.places.PlacesServiceStatus.OK && place?.geometry?.location) {
          const lat = place.geometry.location.lat()
          const lng = place.geometry.location.lng()
          setPosition({ lat, lng })
          setInputValue(place.formatted_address || description)
          setShowDropdown(false)
          onAddressChange({ addressText: place.formatted_address || description, lat, lng })
          sessionTokenRef.current = new google.maps.places.AutocompleteSessionToken()
        }
      }
    )
  }, [onAddressChange])

  const reverseGeocode = useCallback((lat: number, lng: number) => {
    if (!geocoderRef.current) return

    geocoderRef.current.geocode({ location: { lat, lng } }, (results, status) => {
      if (status === google.maps.GeocoderStatus.OK && results?.[0]) {
        const addr = results[0].formatted_address
        setInputValue(addr)
        onAddressChange({ addressText: addr, lat, lng })
      }
    })
  }, [onAddressChange])

  const handleMarkerDragEnd = useCallback((e: any) => {
    if (!e.latLng) return
    const lat = e.latLng.lat()
    const lng = e.latLng.lng()
    setPosition({ lat, lng })
    reverseGeocode(lat, lng)
  }, [reverseGeocode])

  const handleMapClick = useCallback((e: any) => {
    if (!e.detail?.latLng) return
    const lat = e.detail.latLng.lat
    const lng = e.detail.latLng.lng
    setPosition({ lat, lng })
    reverseGeocode(lat, lng)
  }, [reverseGeocode])

  return (
    <APIProvider apiKey={API_KEY}>
      <div className="space-y-4">
        <div className="relative">
          <Label htmlFor="address-input">Địa chỉ nhận sân *</Label>
          <div className="relative mt-2">
            <MapPin className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
            <Input
              id="address-input"
              value={inputValue}
              onChange={handleInputChange}
              onFocus={() => predictions.length > 0 && setShowDropdown(true)}
              placeholder="Nhập địa chỉ hoặc chọn trên bản đồ"
              className="pl-10"
            />
          </div>
          {showDropdown && predictions.length > 0 && (
            <Card className="absolute z-50 w-full mt-1 max-h-60 overflow-y-auto">
              {predictions.map((pred) => (
                <button
                  key={pred.place_id}
                  type="button"
                  onClick={() => selectPrediction(pred.place_id, pred.description)}
                  className="w-full text-left px-4 py-2 hover:bg-muted transition-colors text-sm"
                >
                  {pred.description}
                </button>
              ))}
            </Card>
          )}
        </div>

        <div className="h-[400px] rounded-lg overflow-hidden border">
          <Map
            defaultCenter={VN_CENTER}
            center={position}
            defaultZoom={13}
            gestureHandling="greedy"
            disableDefaultUI={false}
            onClick={handleMapClick}
          >
            <AdvancedMarker position={position} draggable onDragEnd={handleMarkerDragEnd} />
          </Map>
        </div>
      </div>
    </APIProvider>
  )
}
