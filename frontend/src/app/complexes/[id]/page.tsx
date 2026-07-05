'use client'

import { useEffect, useState } from 'react'
import { useParams, useSearchParams, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { Header } from '@/components/layout/Header'
import ComplexDetail from '@/components/complexes/ComplexDetail'
import { getComplexDetail, getComplexFacilities, getFacilityCourts } from '@/lib/api/complex'
import { useSession } from 'next-auth/react'
import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { IconBuildingStadium } from '@tabler/icons-react'
import type { FacilityDto } from '@/types/complex'

export default function ComplexDetailPage() {
  const params = useParams()
  const searchParams = useSearchParams()
  const router = useRouter()
  const { data: session } = useSession()

  const complexId = parseInt(params.id as string, 10)
  const defaultCourtId = searchParams.get('courtId')
    ? parseInt(searchParams.get('courtId')!, 10)
    : null

  // Track selected facility (L2)
  const [selectedFacilityId, setSelectedFacilityId] = useState<number | null>(null)

  // 1. Fetch complex detail
  const { data: complex, isLoading: complexLoading } = useQuery({
    queryKey: ['complex', complexId],
    queryFn: () => getComplexDetail(complexId),
    enabled: !isNaN(complexId),
    staleTime: 300_000,
  })

  // 2. Fetch facilities (L2)
  const { data: facilities = [], isLoading: facilitiesLoading } = useQuery({
    queryKey: ['complex-facilities', complexId],
    queryFn: () => getComplexFacilities(complexId),
    enabled: !isNaN(complexId),
    staleTime: 180_000,
  })

  // Filter facilities based on URL param
  const sportTypeIdParam = searchParams.get('sportTypeId')
  const filterSportTypeId = sportTypeIdParam ? Number(sportTypeIdParam) : null
  const filteredFacilities = filterSportTypeId
    ? facilities.filter(f => !f.sportType || f.sportType.sportTypeId === filterSportTypeId)
    : facilities

  // Reset the selected facility whenever the sport filter no longer includes it
  // (e.g. navigating to the same complex again with a different sportTypeId, which
  // Next.js does not remount for since only the query string changes)
  useEffect(() => {
    if (selectedFacilityId !== null && !filteredFacilities.some(f => f.stadiumId === selectedFacilityId)) {
      setSelectedFacilityId(null)
    }
  }, [selectedFacilityId, filteredFacilities])

  // Auto-select facility when data arrives
  const activeFacilityId = selectedFacilityId ?? (filteredFacilities[0]?.stadiumId ?? null)

  // 3. Fetch courts under selected facility (L3)
  const { data: courts = [], isLoading: courtsLoading } = useQuery({
    queryKey: ['facility-courts', activeFacilityId],
    queryFn: () => getFacilityCourts(activeFacilityId!),
    enabled: activeFacilityId !== null,
    staleTime: 60_000,
  })

  // Loading state
  if (complexLoading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header />
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6 animate-pulse">
          <div className="h-[380px] bg-gray-200 rounded-2xl" />
          <div className="h-12 bg-gray-200 rounded-xl w-2/3" />
          <div className="flex gap-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-20 w-40 bg-gray-200 rounded-xl" />
            ))}
          </div>
          <div className="space-y-3">
            {[1, 2].map((i) => (
              <div key={i} className="h-16 bg-gray-200 rounded-xl" />
            ))}
          </div>
        </div>
      </div>
    )
  }

  if (!complex) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <Header />
        <div className="text-center">
          <IconBuildingStadium className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h2 className="text-lg font-semibold text-gray-700 mb-2">
            Không tìm thấy cơ sở sân
          </h2>
          <p className="text-sm text-gray-400 mb-5">
            Cơ sở này không tồn tại hoặc đã bị xóa.
          </p>
          <Button asChild>
            <Link href="/search">Quay lại tìm kiếm</Link>
          </Button>
        </div>
      </div>
    )
  }

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
