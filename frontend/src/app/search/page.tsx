'use client'

import { useState, useEffect, Suspense } from 'react'
import { useRouter, usePathname, useSearchParams } from 'next/navigation'
import { searchStadiums, getAmenities, getSportTypes, StadiumResponse, StadiumSearchRequest, Amenity } from '@/lib/api/stadium'
import { Button } from '@/components/ui/button'
import { Map, X } from 'lucide-react'
import dynamic from 'next/dynamic'

// Import New Components
import { HorizontalSearch } from './components/HorizontalSearch'
import { SportTypeTabs } from './components/SportTypeTabs'
import { FilterModal } from './components/FilterModal'
import { StadiumCard } from './components/StadiumCard'

const StadiumMapModal = dynamic(() => import('./components/StadiumMapModal'), {
  ssr: false,
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

export default function SearchPage() {
  return (
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center">Đang tải...</div>}>
      <SearchPageContent />
    </Suspense>
  )
}

function SearchPageContent() {
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()

  const [stadiums, setStadiums] = useState<StadiumResponse[]>([])
  const [amenitiesList, setAmenitiesList] = useState<Amenity[]>([])
  const [sportTypes, setSportTypes] = useState<{ sportTypeId: number, sportName: string }[]>([])
  const [loading, setLoading] = useState(false)
  const [isMapOpen, setIsMapOpen] = useState(false)

  // Local state for UI inputs (initialized safely to avoid SSR Hydration mismatch)
  const [filters, setFilters] = useState<StadiumSearchRequest>({
    keyword: '',
    sportTypeId: undefined,
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
    size: 12,
  })

  const debouncedFilters = useDebounce(filters, 500)

  // 1. Fetch amenities and sports
  useEffect(() => {
    Promise.all([getAmenities(), getSportTypes()])
      .then(([amenitiesRes, sportTypesRes]) => {
        setAmenitiesList(amenitiesRes)
        setSportTypes(sportTypesRes)
      })
      .catch(console.error)
  }, [])

  // 2. Sync debounced state to URL
  useEffect(() => {
    const params = new URLSearchParams()
    
    Object.entries(debouncedFilters).forEach(([key, value]) => {
      if (value !== undefined && value !== '' && key !== 'amenityIds') {
        params.append(key, String(value))
      }
    })
    
    if (debouncedFilters.amenityIds && debouncedFilters.amenityIds.length > 0) {
      debouncedFilters.amenityIds.forEach(id => {
        params.append('amenityIds', String(id))
      })
    }

    router.replace(`${pathname}?${params.toString()}`, { scroll: false })
  }, [debouncedFilters, pathname, router])

  // 3. Single Source of Truth: Fetch from URL changes & Sync UI on Back/Forward
  useEffect(() => {
    const currentFilters: StadiumSearchRequest = {
      keyword: searchParams.get('keyword') || '',
      sportTypeId: searchParams.get('sportTypeId') ? Number(searchParams.get('sportTypeId')) : undefined,
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
      size: searchParams.has('size') ? Number(searchParams.get('size')) : 12,
    }

    // Deep compare to sync UI properly for Back/Forward avoiding infinite loop
    setFilters(prev => {
      if (JSON.stringify(prev) === JSON.stringify(currentFilters)) {
        return prev;
      }
      return currentFilters;
    })

    // Prepare clean filters for API
    const cleanFilters: Partial<StadiumSearchRequest> = { ...currentFilters }
    if (!cleanFilters.targetDate) delete cleanFilters.targetDate
    if (!cleanFilters.startTime) delete cleanFilters.startTime
    if (!cleanFilters.endTime) delete cleanFilters.endTime
    if (!cleanFilters.keyword) delete cleanFilters.keyword
    if (cleanFilters.sportTypeId === undefined) delete cleanFilters.sportTypeId

    const fetchStadiums = async () => {
      setLoading(true)
      try {
        const res = await searchStadiums(cleanFilters as StadiumSearchRequest)
        setStadiums(res.content)
      } catch (error) {
        console.error(error)
      } finally {
        setLoading(false)
      }
    }
    
    fetchStadiums()
  }, [searchParams])

  const handleFilterChange = (key: keyof StadiumSearchRequest, value: StadiumSearchRequest[keyof StadiumSearchRequest]) => {
    setFilters(prev => ({ ...prev, [key]: value, page: key !== 'page' ? 0 : value as number }))
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
      amenityIds: [],
      userLat: undefined,
      userLng: undefined,
      radiusInKm: undefined,
      minPrice: 0,
      maxPrice: 1000000,
      page: 0,
      size: 12,
    })
  }

  const getLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setFilters(prev => ({
            ...prev,
            userLat: position.coords.latitude,
            userLng: position.coords.longitude,
            radiusInKm: 15,
            page: 0
          }))
        },
        (error) => alert("Không thể lấy vị trí của bạn. Vui lòng cấp quyền.")
      )
    } else {
      alert("Trình duyệt của bạn không hỗ trợ định vị")
    }
  }

  return (
    <div className="min-h-screen bg-gray-50/50 dark:bg-background pb-20">

      {/* 1. Hero Banner */}
      <div className="relative h-[300px] md:h-[400px] w-full bg-gray-900 overflow-hidden">
        <img
          src="https://images.unsplash.com/photo-1579952363873-27f3bade9f55?q=80&w=2000&auto=format&fit=crop"
          alt="Sport Banner"
          className="w-full h-full object-cover opacity-60"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-background/90 to-transparent"></div>
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center px-4">
          <h1 className="text-4xl md:text-5xl lg:text-6xl font-extrabold text-white tracking-tight mb-4 drop-shadow-lg">
            Khám phá Sân Thể Thao Đỉnh Cao
          </h1>
          <p className="text-lg md:text-xl text-gray-200 font-medium max-w-2xl drop-shadow-md">
            Tìm kiếm, xem giá và đặt sân ngay lập tức với hàng trăm lựa chọn tốt nhất xung quanh bạn.
          </p>
        </div>
      </div>

      {/* 2. Horizontal Search Bar */}
      <HorizontalSearch
        filters={filters}
        onFilterChange={handleFilterChange}
        onGetLocation={getLocation}
      />

      {/* 3. Sport Type Tabs */}
      <SportTypeTabs
        sportTypes={sportTypes}
        selectedId={filters.sportTypeId}
        onSelect={(id) => handleFilterChange('sportTypeId', id)}
      />

      <div className="container mx-auto px-4 sm:px-6 lg:px-8 max-w-7xl">

        {/* 4. Filter Info & Modal Trigger */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
          <div>
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Danh sách sân</h2>
            <p className="text-muted-foreground text-sm mt-1">Tìm thấy <strong className="text-foreground">{stadiums.length}</strong> sân phù hợp với bạn</p>
          </div>

          <div className="flex gap-3">
            <Button variant="ghost" size="sm" onClick={handleClearFilters} className="text-muted-foreground hover:text-destructive hidden sm:flex">
              <X className="h-4 w-4 mr-1" /> Xóa bộ lọc
            </Button>

            <FilterModal
              filters={filters}
              amenitiesList={amenitiesList}
              totalResults={stadiums.length}
              onFilterChange={handleFilterChange}
              onAmenityToggle={handleAmenityToggle}
              onClearFilters={handleClearFilters}
            />

            <Button
              variant="outline"
              onClick={() => setIsMapOpen(true)}
              className="border-gray-200 dark:border-border font-semibold hover:bg-gray-50 dark:hover:bg-muted shadow-sm"
            >
              <Map className="mr-2 h-4 w-4" /> Bản đồ
            </Button>
          </div>
        </div>

        {/* 5. Stadium Grid Area */}
        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3, 4, 5, 6].map(i => (
              <div key={i} className="animate-pulse bg-card rounded-2xl h-[420px] border border-gray-100 dark:border-border"></div>
            ))}
          </div>
        ) : stadiums.length === 0 ? (
          <div className="text-center py-20 bg-white dark:bg-card/50 rounded-3xl border border-dashed border-gray-200 dark:border-border shadow-sm flex flex-col items-center justify-center">
            <div className="w-40 h-40 mb-6 opacity-50">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" className="text-gray-400 w-full h-full">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z" strokeLinecap="round" strokeLinejoin="round" />
                <path d="M12 6v6l4 4" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </div>
            <h3 className="text-2xl font-bold mb-3 text-gray-900 dark:text-white">Không tìm thấy sân</h3>
            <p className="text-muted-foreground mb-8 max-w-md">Rất tiếc, chúng tôi không tìm thấy sân nào khớp với điều kiện lọc của bạn. Vui lòng thử nới lỏng các yêu cầu nhé!</p>
            <Button onClick={handleClearFilters} className="bg-gray-900 hover:bg-gray-800 text-white dark:bg-white dark:text-gray-900 dark:hover:bg-gray-100 rounded-full px-8 py-6 text-base font-bold shadow-lg transition-all hover:scale-105 active:scale-95">
              Xóa tất cả bộ lọc
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {stadiums.map((stadium) => (
              <StadiumCard
                key={stadium.stadiumId}
                stadium={stadium}
                isUrgent={false}
              />
            ))}
          </div>
        )}

      </div>

      <StadiumMapModal
        isOpen={isMapOpen}
        onClose={() => setIsMapOpen(false)}
        stadiums={stadiums}
      />
    </div>
  )
}
