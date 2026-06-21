'use client'

import { useQuery } from '@tanstack/react-query'
import Link from 'next/link'
import { getVenueDetail } from '@/lib/api/venue'
import VenueDetail from '@/components/venues/VenueDetail'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Header } from '@/components/layout/Header'

export default function VenueDetailPage({ params }: { params: { id: string } }) {
  const venueId = parseInt(params.id)

  const { data: venue, isLoading, error } = useQuery({
    queryKey: ['venue-detail', venueId],
    queryFn: () => getVenueDetail(venueId),
    staleTime: 300_000,
  })

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="max-w-[768px] w-full p-4 space-y-4">
          <div className="h-[240px] animate-pulse rounded-lg bg-gray-200" />
          <div className="grid grid-cols-5 gap-1 h-16">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="animate-pulse rounded bg-gray-200 h-full" />
            ))}
          </div>
          <div className="h-14 animate-pulse rounded-lg bg-gray-200" />
          <div className="grid grid-cols-2 gap-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-16 animate-pulse rounded-lg bg-gray-200" />
            ))}
          </div>
        </div>
      </div>
    )
  }

  if (error || !venue) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <Card className="p-8 text-center max-w-sm w-full border border-gray-200 rounded-lg bg-white shadow-none">
          <h2 className="text-lg font-medium text-gray-700 mb-2">Không tìm thấy sân</h2>
          <p className="text-sm text-gray-400 mb-5">Sân bạn đang tìm không tồn tại hoặc đã bị xóa.</p>
          <Button asChild className="bg-[#1a8a4a] hover:bg-[#15713c] text-white">
            <Link href="/search">Quay lại tìm kiếm</Link>
          </Button>
        </Card>
      </div>
    )
  }

  const formatTimeNum = (timeStr: string) => {
    try {
      const parts = timeStr.split(':')
      return `${parts[0]}:${parts[1]}`
    } catch {
      return timeStr
    }
  }

  // Map backend response data structure to VenueDetail props structure
  const images = venue.imageUrls || []

  const mappedVenue = {
    id: venue.stadiumId,
    name: venue.stadiumName,
    sport: venue.sportName,
    address: venue.address,
    rating: venue.averageRating || 0,
    reviewCount: venue.totalReviews || 0,
    pricePerHour: venue.pricePerHour,
    openTime: formatTimeNum(venue.openTime),
    closeTime: formatTimeNum(venue.closeTime),
    status: venue.stadiumStatus,
    description: venue.description,
    images: images,
    amenities: venue.amenities?.map((a) => a.name) || [],
    timeSlots: venue.timeSlots?.map((s) => {
      const parts = s.startTime.split('T')
      const date = parts[0]
      const hour = parts[1] ? parts[1].substring(0, 5) : '08:00'
      return {
        date,
        hour,
        available: s.slotStatus === 'AVAILABLE',
      }
    }) || [],
    services: venue.accessories?.map((a) => ({
      name: a.name,
      stock: a.quantity,
      price: a.pricePerUnit,
    })) || [],
    owner: {
      name: venue.owner?.ownerName || 'N/A',
      initials: venue.owner?.ownerName
        ? venue.owner.ownerName.split(' ').slice(-2).map((w: string) => w[0]).join('')
        : '??',
      phone: venue.owner?.phoneNumber || 'N/A',
    },
    coordinates: {
      lat: venue.latitude || 0,
      lng: venue.longitude || 0,
    },
    recentReviews: venue.recentReviews?.map((r) => ({
      reviewId: r.reviewId,
      userId: r.userId,
      userName: r.userName,
      userAvatar: r.userAvatar ?? null,
      ratingScore: r.ratingScore,
      comment: r.comment,
      ownerResponse: r.ownerResponse ?? null,
      createdAt: r.createdAt,
    })) || [],
  }

  return (
    <div className="min-h-screen bg-white">
      <Header />
      <VenueDetail venue={mappedVenue} />
    </div>
  )
}

