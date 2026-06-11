'use client'

import { useEffect, useRef } from 'react'
import type L from 'leaflet'

interface VenueMapProps {
  latitude: number
  longitude: number
  venueName: string
  height?: string
}

export default function VenueMap({ latitude, longitude, venueName, height }: VenueMapProps) {
  const mapRef = useRef<HTMLDivElement>(null)
  const mapInstanceRef = useRef<L.Map | null>(null)

  useEffect(() => {
    let isCancelled = false

    if (!mapRef.current) return

    const initMap = async () => {
      // Dynamic import to avoid SSR issues
      const Leaflet = (await import('leaflet')).default
      if (isCancelled) return

      // Inject leaflet CSS if not already present
      if (!document.getElementById('leaflet-css')) {
        const link = document.createElement('link')
        link.id = 'leaflet-css'
        link.rel = 'stylesheet'
        link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'
        document.head.appendChild(link)
      }

      if (!mapRef.current) return

      // Double check in case another instance got initialized (e.g. double-render)
      if ((mapRef.current as any)._leaflet_id) return

      const map = Leaflet.map(mapRef.current).setView([latitude, longitude], 15)
      mapInstanceRef.current = map

      Leaflet.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      }).addTo(map)

      const icon = Leaflet.icon({
        iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
        shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41],
      })

      Leaflet.marker([latitude, longitude], { icon })
        .addTo(map)
        .bindPopup(`<b>${venueName}</b>`)
        .openPopup()
    }

    initMap()

    return () => {
      isCancelled = true
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove()
        mapInstanceRef.current = null
      }
    }
  }, [latitude, longitude, venueName])

  return (
    <div
      ref={mapRef}
      className={`w-full rounded-lg z-0 ${height || 'h-64'}`}
      style={{ minHeight: height ? undefined : '256px' }}
    />
  )
}

