import { Suspense } from 'react'
import { getAmenities, getSportTypes } from '@/lib/api/stadium'
import { searchComplexes } from '@/lib/api/complex'
import { getLocations } from '@/lib/api/location'
import type { ComplexSearchParams, StadiumComplexDto } from '@/types/complex'
import SearchPageClient from './components/SearchPageClient'

export default async function SearchPage({
  searchParams,
}: {
  searchParams: Record<string, string | string[] | undefined>
}) {
  // 1. Parallel fetch static reference data on server
  const [initialAmenities, initialSportTypes, initialLocations] = await Promise.all([
    getAmenities().catch(() => []),
    getSportTypes().catch(() => []),
    getLocations().catch(() => []),
  ])

  // 2. Parse search parameters safely
  const keyword = typeof searchParams.keyword === 'string' ? searchParams.keyword : ''
  const sportTypeId = searchParams.sportTypeId ? Number(searchParams.sportTypeId) : undefined
  const province = typeof searchParams.province === 'string' ? searchParams.province : ''
  const district = typeof searchParams.district === 'string' ? searchParams.district : ''
  const targetDate = typeof searchParams.targetDate === 'string' ? searchParams.targetDate : ''
  const startTime = typeof searchParams.startTime === 'string' ? searchParams.startTime : ''
  const endTime = typeof searchParams.endTime === 'string' ? searchParams.endTime : ''
  
  let amenityIds: number[] = []
  if (searchParams.amenityIds) {
    if (Array.isArray(searchParams.amenityIds)) {
      amenityIds = searchParams.amenityIds.map(Number)
    } else {
      amenityIds = [Number(searchParams.amenityIds)]
    }
  }
  
  const userLat = searchParams.userLat ? Number(searchParams.userLat) : undefined
  const userLng = searchParams.userLng ? Number(searchParams.userLng) : undefined
  const radiusInKm = searchParams.radiusInKm ? Number(searchParams.radiusInKm) : undefined
  const minPrice = searchParams.minPrice ? Number(searchParams.minPrice) : 0
  const maxPrice = searchParams.maxPrice ? Number(searchParams.maxPrice) : 1000000
  const page = searchParams.page ? Number(searchParams.page) : 0
  const size = searchParams.size ? Number(searchParams.size) : 100

  const initialFilters: ComplexSearchParams = {
    keyword,
    sportTypeId,
    province,
    district,
    targetDate,
    startTime,
    endTime,
    amenityIds,
    userLat,
    userLng,
    radiusInKm,
    minPrice,
    maxPrice,
    page,
    size,
  }

  // 3. Fetch initial complexes on server side
  const cleanFilters: Partial<ComplexSearchParams> = { ...initialFilters }
  if (!cleanFilters.targetDate) delete cleanFilters.targetDate
  if (!cleanFilters.startTime) delete cleanFilters.startTime
  if (!cleanFilters.endTime) delete cleanFilters.endTime
  if (!cleanFilters.keyword) delete cleanFilters.keyword
  if (cleanFilters.sportTypeId === undefined) delete cleanFilters.sportTypeId
  if (!cleanFilters.province) delete cleanFilters.province
  if (!cleanFilters.district) delete cleanFilters.district
  if (cleanFilters.minPrice === 0) delete cleanFilters.minPrice
  if (cleanFilters.maxPrice === 1000000) delete cleanFilters.maxPrice

  let initialComplexes: StadiumComplexDto[] = []
  let initialTotalElements = 0
  try {
    const res = await searchComplexes(cleanFilters as ComplexSearchParams)
    initialComplexes = res.content
    initialTotalElements = res.totalElements
  } catch (error) {
    console.error("Server-side searchComplexes failed:", error)
  }

  return (
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center">Đang tải...</div>}>
      <SearchPageClient
        initialAmenities={initialAmenities}
        initialSportTypes={initialSportTypes}
        initialLocations={initialLocations}
        initialComplexes={initialComplexes}
        initialTotalElements={initialTotalElements}
        initialFilters={initialFilters}
      />
    </Suspense>
  )
}
