/**
 * /venues/[id] — backward compatibility redirect (Server Component).
 *
 * Looks up the complex that owns this court and issues a true HTTP 307
 * redirect so the browser never sees the old page (no client-side flash).
 */
import { redirect, notFound } from 'next/navigation'
import { getVenueDetail } from '@/lib/api/venue'
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

  let venue
  try {
    venue = await getVenueDetail(venueId)
  } catch {
    notFound()
  }

  // If stadium belongs to a complex hierarchy, redirect to new complex page
  // preserving the courtId as a query param so the accordion auto-opens
  if (venue && (venue as unknown as { complexId?: number }).complexId) {
    const complexId = (venue as unknown as { complexId: number }).complexId
    redirect(`/complexes/${complexId}?courtId=${venueId}`)
  }

  if (!venue) notFound()

  // Fallback: render legacy detail view for stadiums not yet migrated
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
