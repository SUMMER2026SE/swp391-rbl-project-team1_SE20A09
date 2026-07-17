'use client'

import { useEffect, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { Header } from '@/components/layout/Header'
import ComplexDetail from '@/components/complexes/ComplexDetail'
import { getFacilityCourts } from '@/lib/api/complex'
import type { StadiumComplexDto, FacilityDto } from '@/types/complex'

interface ComplexDetailClientProps {
  complex: StadiumComplexDto
  initialFacilities: FacilityDto[]
}

export default function ComplexDetailClient({
  complex,
  initialFacilities,
}: ComplexDetailClientProps) {
  const searchParams = useSearchParams()

  // Track selected facility (L2)
  const [selectedFacilityId, setSelectedFacilityId] = useState<number | null>(null)

  // Filter facilities based on URL param
  const sportTypeIdParam = searchParams.get('sportTypeId')
  const filterSportTypeId = sportTypeIdParam ? Number(sportTypeIdParam) : null
  const filteredFacilities = filterSportTypeId
    ? initialFacilities.filter(f => !f.sportType || f.sportType.sportTypeId === filterSportTypeId)
    : initialFacilities

  // Reset the selected facility whenever the sport filter no longer includes it
  useEffect(() => {
    if (selectedFacilityId !== null && !filteredFacilities.some(f => f.stadiumId === selectedFacilityId)) {
      setSelectedFacilityId(null)
    }
  }, [selectedFacilityId, filteredFacilities])

  // Auto-select facility when data arrives
  const activeFacilityId = selectedFacilityId ?? (filteredFacilities[0]?.stadiumId ?? null)

  // Fetch courts under selected facility (L3)
  const { data: courts = [], isLoading: courtsLoading } = useQuery({
    queryKey: ['facility-courts', activeFacilityId],
    queryFn: () => getFacilityCourts(activeFacilityId!),
    enabled: activeFacilityId !== null,
    staleTime: 60_000,
  })

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Header />
      <ComplexDetail
        complex={complex}
        facilities={filteredFacilities}
        activeFacilityId={activeFacilityId}
        setActiveFacilityId={setSelectedFacilityId}
        courts={courts}
        courtsLoading={courtsLoading}
      />
    </div>
  )
}
