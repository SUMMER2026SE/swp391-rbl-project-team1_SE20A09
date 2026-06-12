'use client'

import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet'
import { useRef } from 'react'

interface LeafletMapProps {
  position: { lat: number; lng: number }
  onMapClick: (lat: number, lng: number) => void
  onDragEnd: (lat: number, lng: number) => void
}

function MapEventHandler({ onMapClick }: { onMapClick: (lat: number, lng: number) => void }) {
  useMapEvents({
    click(e: any) {
      onMapClick(e.latlng.lat, e.latlng.lng)
    },
  })
  return null
}

function DraggableMarkerComponent({
  position,
  onDragEnd,
}: {
  position: [number, number]
  onDragEnd: (lat: number, lng: number) => void
}) {
  const markerRef = useRef<any>(null)

  const eventHandlers = {
    dragend() {
      const marker = markerRef.current
      if (marker) {
        const { lat, lng } = marker.getLatLng()
        onDragEnd(lat, lng)
      }
    },
  }

  return <Marker ref={markerRef} position={position} draggable eventHandlers={eventHandlers} />
}

export default function LeafletMap({ position, onMapClick, onDragEnd }: LeafletMapProps) {
  return (
    <MapContainer center={[position.lat, position.lng]} zoom={15} style={{ height: '100%', width: '100%' }}>
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <DraggableMarkerComponent position={[position.lat, position.lng]} onDragEnd={onDragEnd} />
      <MapEventHandler onMapClick={onMapClick} />
    </MapContainer>
  )
}
