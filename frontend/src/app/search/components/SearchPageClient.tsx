'use client'

import { useState, useEffect, useRef } from 'react'
import { useRouter, usePathname, useSearchParams } from 'next/navigation'
import { Amenity } from '@/lib/api/stadium'
import { searchComplexes } from '@/lib/api/complex'
import { SupportedLocationDto } from '@/lib/api/location'
import type { StadiumComplexDto, ComplexSearchParams } from '@/types/complex'
import { Button } from '@/components/ui/button'
import { X } from 'lucide-react'
import dynamic from 'next/dynamic'
import { toast } from 'sonner'

// Import components
import { HorizontalSearch } from './HorizontalSearch'
import { SportTypeTabs } from './SportTypeTabs'
import { FilterModal } from './FilterModal'
import { ComplexCard } from './ComplexCard'
import { Header } from '@/components/layout/Header'

// Import Pagination
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination"

const ComplexMap = dynamic(() => import('./ComplexMap'), {
  ssr: false,
  loading: () => <div className="w-full h-full bg-muted animate-pulse rounded-2xl" />
})

// Hook debounce
function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value)
  useEffect(() => {
    const handler = setTimeout(() => setDebouncedValue(value), delay)
    return () => clearTimeout(handler)
  }, [value, delay])
  return debouncedValue
}

function buildSearchParams(filters: ComplexSearchParams): URLSearchParams {
  const params = new URLSearchParams()

  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '' && key !== 'amenityIds') {
      if (key === 'minPrice' && Number(value) === 0) return
      if (key === 'maxPrice' && Number(value) === 1000000) return
      if (key === 'page' && Number(value) === 0) return
      if (key === 'size' && Number(value) === 100) return
      params.append(key, String(value))
    }
  })

  if (filters.amenityIds && filters.amenityIds.length > 0) {
    filters.amenityIds.forEach(id => {
      params.append('amenityIds', String(id))
    })
  }

  return params
}

interface SearchPageClientProps {
  initialAmenities: Amenity[]
  initialSportTypes: { sportTypeId: number; sportName: string }[]
  initialLocations: SupportedLocationDto[]
  initialComplexes: StadiumComplexDto[]
  initialTotalElements: number
  initialFilters: ComplexSearchParams
}

export default function SearchPageClient({
  initialAmenities,
  initialSportTypes,
  initialLocations,
  initialComplexes,
  initialTotalElements,
  initialFilters,
}: SearchPageClientProps) {
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()

  const [complexes, setComplexes] = useState<StadiumComplexDto[]>(initialComplexes)
  const [amenitiesList] = useState<Amenity[]>(initialAmenities)
  const [sportTypes] = useState<{ sportTypeId: number; sportName: string }[]>(initialSportTypes)
  const [locations] = useState<SupportedLocationDto[]>(initialLocations)
  const [loading, setLoading] = useState(false)
  const [isLocating, setIsLocating] = useState(false)

  const [totalElements, setTotalElements] = useState(initialTotalElements)
  const [listPage, setListPage] = useState(0)
  const LIST_PAGE_SIZE = 10

  const [hoveredComplexId, setHoveredComplexId] = useState<number | null>(null)

  const [filters, setFilters] = useState<ComplexSearchParams>(initialFilters)

  const debouncedFilters = useDebounce(filters, 500)
  const isFirstMount = useRef(true)

  // 1. Sync debounced state to URL
  useEffect(() => {
    if (isFirstMount.current) return
    const params = buildSearchParams(debouncedFilters)
    router.replace(`${pathname}?${params.toString()}`, { scroll: false })
  }, [debouncedFilters, pathname, router])

  // 2. Listen to URL changes and fetch complexes
  useEffect(() => {
    const currentFilters: ComplexSearchParams = {
      keyword: searchParams.get('keyword') || '',
      sportTypeId: searchParams.get('sportTypeId') ? Number(searchParams.get('sportTypeId')) : undefined,
      province: searchParams.get('province') || '',
      district: searchParams.get('district') || '',
      targetDate: searchParams.get('targetDate') || '',
      startTime: searchParams.get('startTime') || '',
      endTime: searchParams.get('endTime') || '',
      amenityIds: searchParams.getAll('amenityIds').map(Number),
      userLat: searchParams.get('userLat') ? Number(searchParams.get('userLat')) : undefined,
      userLng: searchParams.get('userLng') ? Number(searchParams.get('userLng')) : undefined,
      radiusInKm: searchParams.get('radiusInKm') ? Number(searchParams.get('radiusInKm')) : undefined,
      minPrice: searchParams.get('minPrice') ? Number(searchParams.get('minPrice')) : 0,
      maxPrice: searchParams.get('maxPrice') ? Number(searchParams.get('maxPrice')) : 1000000,
      page: searchParams.has('page') ? Number(searchParams.get('page')) : 0,
      size: searchParams.has('size') ? Number(searchParams.get('size')) : 100,
    }

    setFilters(prev => {
      if (JSON.stringify(prev) === JSON.stringify(currentFilters)) {
        return prev
      }
      return currentFilters
    })

    if (isFirstMount.current) {
      isFirstMount.current = false
      return
    }

    // Prepare clean filters for API
    const cleanFilters: Partial<ComplexSearchParams> = { ...currentFilters }
    if (!cleanFilters.targetDate) delete cleanFilters.targetDate
    if (!cleanFilters.startTime) delete cleanFilters.startTime
    if (!cleanFilters.endTime) delete cleanFilters.endTime
    if (!cleanFilters.keyword) delete cleanFilters.keyword
    if (cleanFilters.sportTypeId === undefined) delete cleanFilters.sportTypeId
    if (!cleanFilters.province) delete cleanFilters.province
    if (!cleanFilters.district) delete cleanFilters.district
    if (cleanFilters.minPrice === 0) delete cleanFilters.minPrice
    if (cleanFilters.maxPrice === 1000000) delete cleanFilters.maxPrice

    const fetchComplexesData = async () => {
      setLoading(true)
      try {
        const res = await searchComplexes(cleanFilters as ComplexSearchParams)
        setComplexes(res.content)
        setTotalElements(res.totalElements)
        setListPage(0)
      } catch (error) {
        console.error(error)
      } finally {
        setLoading(false)
      }
    }

    fetchComplexesData()
  }, [searchParams])

  const handleFilterChange = (key: keyof ComplexSearchParams, value: ComplexSearchParams[keyof ComplexSearchParams]) => {
    setFilters(prev => ({ ...prev, [key]: value, page: key !== 'page' ? 0 : value as number }))
  }

  const handleSearchNow = () => {
    const params = buildSearchParams(filters)
    router.replace(`${pathname}?${params.toString()}`, { scroll: false })
  }

  const handleAmenityToggle = (id: number) => {
    setFilters(prev => {
      const ids = prev.amenityIds || []
      const newIds = ids.includes(id) ? ids.filter(i => i !== id) : [...ids, id]
      return { ...prev, amenityIds: newIds, page: 0 }
    })
  }

  const handleClearFilters = () => {
    setFilters({
      keyword: '',
      sportTypeId: undefined,
      province: '',
      district: '',
      targetDate: '',
      startTime: '',
      endTime: '',
      amenityIds: [],
      userLat: undefined,
      userLng: undefined,
      radiusInKm: undefined,
      minPrice: 0,
      maxPrice: 1000000,
      page: 0,
      size: 100,
    })
  }

  const getLocation = () => {
    if (navigator.geolocation) {
      setIsLocating(true)
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setIsLocating(false)
          setFilters(prev => ({
            ...prev,
            userLat: position.coords.latitude,
            userLng: position.coords.longitude,
            radiusInKm: 15,
            page: 0
          }))
          toast.success("Đã định vị thành công vị trí của bạn!")
        },
        () => {
          setIsLocating(false)
          toast.error("Không thể lấy vị trí của bạn. Vui lòng cấp quyền.")
        }
      )
    } else {
      toast.error("Trình duyệt của bạn không hỗ trợ định vị")
    }
  }

  return (
    <div className="min-h-screen bg-gray-50/50 dark:bg-background pb-20">
      <Header />

      {/* 1. Hero Banner */}
      <div className="relative h-[300px] md:h-[400px] w-full bg-emerald-900 overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-emerald-900 via-emerald-950 to-black" />
        <div className="absolute inset-0 bg-gradient-to-t from-background/90 to-transparent"></div>
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center px-4">
          <h1 className="text-4xl md:text-5xl lg:text-6xl font-extrabold text-white tracking-tight mb-4 drop-shadow-lg">
            Khám phá Tổ Hợp Thể Thao Đỉnh Cao
          </h1>
          <p className="text-lg md:text-xl text-gray-200 font-medium max-w-2xl drop-shadow-md">
            Tìm kiếm tổ hợp sân, xem giá và đặt lịch nhanh chóng với đầy đủ các môn thể thao.
          </p>
        </div>
      </div>

      {/* 2. Horizontal Search Bar */}
      <HorizontalSearch
        filters={filters}
        onFilterChange={handleFilterChange}
        onGetLocation={getLocation}
        onSearch={handleSearchNow}
        isLocating={isLocating}
        locations={locations}
      />

      {/* 3. Sport Type Tabs */}
      <SportTypeTabs
        sportTypes={sportTypes}
        selectedId={filters.sportTypeId}
        onSelect={(id) => handleFilterChange('sportTypeId', id)}
      />

      <div className="container mx-auto px-4 sm:px-6 lg:px-8 max-w-[1600px] mt-6">

        {/* 4. Filter Info & Modal Trigger */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
          <div>
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Danh sách sân</h2>
            <p className="text-muted-foreground text-sm mt-1">
              Tìm thấy <strong className="text-foreground">{totalElements}</strong> tổ hợp phù hợp với bạn
            </p>
          </div>

          <div className="flex gap-3">
            <Button variant="ghost" size="sm" onClick={handleClearFilters} className="text-muted-foreground hover:text-destructive hidden sm:flex">
              <X className="h-4 w-4 mr-1" /> Xóa bộ lọc
            </Button>

            <FilterModal
              filters={filters}
              amenitiesList={amenitiesList}
              totalResults={totalElements}
              onFilterChange={handleFilterChange}
              onAmenityToggle={handleAmenityToggle}
              onClearFilters={handleClearFilters}
            />
          </div>
        </div>

        {/* 5. Split-Screen Layout: List + Map */}
        <div className="flex flex-col lg:flex-row gap-6">
          
          {/* Left Column: Complex List */}
          <div className="flex-1 lg:w-[55%] xl:w-[60%] flex flex-col gap-6">
            {loading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                {[1, 2, 3, 4, 5, 6].map(i => (
                  <div key={i} className="animate-pulse bg-card rounded-2xl h-[420px] border border-gray-100 dark:border-border"></div>
                ))}
              </div>
            ) : complexes.length === 0 ? (
              <div className="text-center py-20 bg-white dark:bg-card/50 rounded-3xl border border-dashed border-gray-200 dark:border-border shadow-sm flex flex-col items-center justify-center">
                <div className="w-40 h-40 mb-6 opacity-50">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" className="text-gray-400 w-full h-full">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z" strokeLinecap="round" strokeLinejoin="round" />
                    <path d="M12 6v6l4 4" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </div>
                <h3 className="text-2xl font-bold mb-3 text-gray-900 dark:text-white">Không tìm thấy tổ hợp</h3>
                <p className="text-muted-foreground mb-8 max-w-md">Rất tiếc, chúng tôi không tìm thấy tổ hợp nào khớp với điều kiện lọc của bạn. Vui lòng thử nới lỏng các yêu cầu nhé!</p>
                <Button onClick={handleClearFilters} className="bg-gray-900 hover:bg-gray-800 text-white dark:bg-white dark:text-gray-900 dark:hover:bg-gray-100 rounded-full px-8 py-6 text-base font-bold shadow-lg transition-all hover:scale-105 active:scale-95">
                  Xóa tất cả bộ lọc
                </Button>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                  {complexes.slice(listPage * LIST_PAGE_SIZE, (listPage + 1) * LIST_PAGE_SIZE).map((complex) => (
                    <div
                      key={complex.complexId}
                      onMouseEnter={() => setHoveredComplexId(complex.complexId)}
                      onMouseLeave={() => setHoveredComplexId(null)}
                      className="h-full"
                    >
                      <ComplexCard complex={complex} />
                    </div>
                  ))}
                </div>

                {/* Pagination Controls */}
                {(() => {
                  const listTotalPages = Math.ceil(complexes.length / LIST_PAGE_SIZE)
                  if (listTotalPages <= 1) return null
                  return (
                    <div className="mt-8 flex justify-center pb-6">
                      <Pagination>
                        <PaginationContent>
                          <PaginationItem>
                            <PaginationPrevious
                              href="#"
                              onClick={(e) => {
                                e.preventDefault()
                                if (listPage > 0) setListPage(listPage - 1)
                              }}
                              className={listPage === 0 ? "pointer-events-none opacity-50" : "cursor-pointer"}
                            />
                          </PaginationItem>

                          {Array.from({ length: listTotalPages }, (_, idx) => (
                            <PaginationItem key={idx}>
                              <PaginationLink
                                href="#"
                                onClick={(e) => {
                                  e.preventDefault()
                                  setListPage(idx)
                                }}
                                isActive={listPage === idx}
                                className="cursor-pointer"
                              >
                                {idx + 1}
                              </PaginationLink>
                            </PaginationItem>
                          ))}

                          <PaginationItem>
                            <PaginationNext
                              href="#"
                              onClick={(e) => {
                                e.preventDefault()
                                if (listPage < listTotalPages - 1) setListPage(listPage + 1)
                              }}
                              className={listPage === listTotalPages - 1 ? "pointer-events-none opacity-50" : "cursor-pointer"}
                            />
                          </PaginationItem>
                        </PaginationContent>
                      </Pagination>
                    </div>
                  )
                })()}
              </>
            )}
          </div>

          {/* Right Column: Sticky Map */}
          <div className="w-full lg:w-[45%] xl:w-[40%] h-[500px] lg:h-[calc(100vh-140px)] lg:sticky lg:top-24 mb-10 lg:mb-0 rounded-2xl overflow-hidden shadow-sm border border-gray-100">
            <ComplexMap
              complexes={loading ? [] : complexes}
              hoveredComplexId={hoveredComplexId}
              userLat={filters.userLat}
              userLng={filters.userLng}
              radiusInKm={filters.radiusInKm}
              province={filters.province}
            />
          </div>

        </div>
      </div>
    </div>
  )
}
