/**
 * /venues/[id] — backward compatibility redirect (Server Component).
 *
 * 1. Calls getCourtWithComplex() to check if this stadium belongs to a complex.
 *    If yes → HTTP 307 redirect to /complexes/{complexId}?courtId={id}.
 * 2. Falls back to rendering the legacy VenueDetail for unmigrated stadiums.
 */
import { redirect, notFound } from 'next/navigation'
import { getVenueDetail } from '@/lib/api/venue'
import { getCourtWithComplex } from '@/lib/api/complex'
import VenueDetail from '@/components/venues/VenueDetail'
import { Header } from '@/components/layout/Header'

interface PageProps {
  params: { id: string }
  searchParams: Record<string, string | string[] | undefined>
}

export default async function VenueDetailPage({ params }: PageProps) {
  const venueId = parseInt(params.id, 10)

  if (isNaN(venueId)) {
    notFound()
  }

  // --- Fast path: redirect to new complex page if stadium belongs to a complex ---
  // getCourtWithComplex returns null gracefully if the endpoint is not yet
  // implemented (catches all errors), so this is safe to call first.
  const courtRef = await getCourtWithComplex(venueId)
  if (courtRef?.complexId) {
    redirect(`/complexes/${courtRef.complexId}?courtId=${venueId}`)
  }

  // --- Fallback: render legacy VenueDetail for unmigrated stadiums ---
  let venue
  try {
    venue = await getVenueDetail(venueId)
  } catch {
    notFound()
  }

  if (!venue) notFound()

  const formatTimeNum = (timeStr: string) => {
    try {
      const parts = timeStr.split(':')
      return `${parts[0]}:${parts[1]}`
    } catch {
      return timeStr
    }
  }

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
    images: venue.imageUrls || [],
    amenities: venue.amenities?.map((a) => a.name) || [],
    timeSlots: venue.timeSlots?.map((s) => {
      const parts = s.startTime.split('T')
      const date = parts[0]
      const hour = parts[1] ? parts[1].substring(0, 5) : '08:00'
      return { date, hour, available: s.slotStatus === 'AVAILABLE' }
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
