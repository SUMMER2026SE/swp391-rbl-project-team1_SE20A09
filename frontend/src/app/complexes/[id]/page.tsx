'use client'

import { useState } from 'react'
import { useParams, useSearchParams, useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { Header } from '@/components/layout/Header'
import ComplexHeader from '@/components/complexes/ComplexHeader'
import FacilityTabs from '@/components/complexes/FacilityTabs'
import CourtScheduleAccordion from '@/components/complexes/CourtScheduleAccordion'
import { getComplexDetail, getComplexFacilities, getComplexCourts } from '@/lib/api/complex'
import { createBooking } from '@/lib/bookings-api'
import { useSession } from 'next-auth/react'
import { toast } from 'sonner'
import Link from 'next/link'
import { Button } from '@/components/ui/button'
import { IconBuildingStadium, IconChevronRight } from '@tabler/icons-react'
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
    select: (data: FacilityDto[]) => {
      // Auto-select first available facility if none selected
      return data
    },
  })

  // Auto-select facility when data arrives
  const activeFacilityId = selectedFacilityId ?? (facilities[0]?.stadiumId ?? null)

  // 3. Fetch courts under selected facility (L3)
  const { data: courts = [], isLoading: courtsLoading } = useQuery({
    queryKey: ['facility-courts', activeFacilityId],
    queryFn: () => getComplexCourts(activeFacilityId!),
    enabled: activeFacilityId !== null,
    staleTime: 60_000,
  })

  // Booking handler
  const handleSlotSelect = async (slotId: number, date: string, courtId: number) => {
    if (!session?.user) {
      toast.info('Vui lòng đăng nhập để đặt sân')
      router.push(`/auth/login?callbackUrl=/complexes/${complexId}`)
      return
    }
    try {
      await createBooking({
        stadiumId: courtId,
        slotId,
        reservationDate: date,
      })
      toast.success('Đặt sân thành công! Vui lòng kiểm tra đơn của bạn.')
      router.push('/bookings')
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể đặt sân, vui lòng thử lại.'
      toast.error(msg)
    }
  }


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
    <div className="min-h-screen bg-gray-50">
      <Header />

      {/* Breadcrumb */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-4 pb-0">
        <nav className="flex items-center gap-1 text-xs text-gray-400" aria-label="Breadcrumb">
          <Link href="/" className="hover:text-emerald-600 transition-colors">Trang chủ</Link>
          <IconChevronRight className="w-3 h-3" />
          <Link href="/search" className="hover:text-emerald-600 transition-colors">Tìm kiếm</Link>
          <IconChevronRight className="w-3 h-3" />
          <span className="text-gray-600 font-medium line-clamp-1">{complex.name}</span>
        </nav>
      </div>

      {/* Complex Gallery + Info Header */}
      <ComplexHeader complex={complex} />

      {/* Main content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-8">

        {/* Facility selector (L2 tabs) */}
        {facilitiesLoading ? (
          <div className="flex gap-3 animate-pulse">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-20 w-44 bg-gray-200 rounded-xl" />
            ))}
          </div>
        ) : (
          <FacilityTabs
            facilities={facilities}
            selectedId={activeFacilityId}
            onSelect={(id) => setSelectedFacilityId(id)}
          />
        )}

        {/* Courts + schedule (L3 accordion) */}
        <div>
          <h2 className="text-lg font-semibold text-gray-800 mb-4">
            Lịch sân & Đặt chỗ
          </h2>

          {courtsLoading ? (
            <div className="space-y-3 animate-pulse">
              {[1, 2].map((i) => (
                <div key={i} className="h-16 bg-gray-200 rounded-xl" />
              ))}
            </div>
          ) : (
            <CourtScheduleAccordion
              courts={courts}
              defaultOpenId={defaultCourtId}
              onSlotSelect={handleSlotSelect}
            />
          )}
        </div>

        {/* Amenities */}
        {complex.amenities && complex.amenities.length > 0 && (
          <div>
            <h2 className="text-lg font-semibold text-gray-800 mb-3">Tiện ích cơ sở</h2>
            <div className="flex flex-wrap gap-2">
              {complex.amenities.map((a) => (
                <span
                  key={a.amenityId}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-sm bg-white border border-gray-100 rounded-full shadow-sm text-gray-700"
                >
                  {a.icon && <span>{a.icon}</span>}
                  {a.name}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
