/**
 * Complex hierarchy TypeScript type definitions.
 * Follows the 3-tier model: Complex (L1) → Facility (L2) → Court (L3).
 */

export interface SportTypeDto {
  sportTypeId: number
  sportName: string
}

export interface AmenityDto {
  amenityId: number
  name: string
  icon?: string | null
}

export interface ComplexImageDto {
  imageId: number
  imageUrl: string
}

export interface TimeSlotDto {
  slotId: number
  startTime: string   // "HH:mm:ss"
  endTime: string     // "HH:mm:ss"
  slotStatus: 'AVAILABLE' | 'BOOKED' | 'MAINTENANCE'
  pricePerSlot: number
}

export type StadiumStatus = 'AVAILABLE' | 'MAINTENANCE' | 'CLOSED'
export type ComplexStatus = 'AVAILABLE' | 'MAINTENANCE' | 'CLOSED'
export type ApprovedStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface StadiumComplexDto {
  complexId: number
  name: string
  description?: string | null
  address: string
  phone?: string | null
  latitude?: number | null
  longitude?: number | null
  coverImageUrl?: string | null
  complexStatus: ComplexStatus
  approvedStatus: ApprovedStatus
  sportTypes: SportTypeDto[]
  amenities: AmenityDto[]
  images: ComplexImageDto[]
  averageRating: number
  totalReviews?: number
  ownerName?: string | null
  ownerPhone?: string | null
  distanceInKm?: number | null
  minPrice?: number | null
  maxPrice?: number | null
}

export interface FacilityDto {
  stadiumId: number
  stadiumName: string
  description?: string | null
  sportType: SportTypeDto
  openTime: string   // "HH:mm:ss"
  closeTime: string  // "HH:mm:ss"
  stadiumStatus: StadiumStatus
  /** True nếu bị chặn đặt HÔM NAY do bảo trì (kể cả cascade từ Complex cha) — dù stadiumStatus vẫn AVAILABLE. */
  underMaintenanceToday?: boolean
  imageUrls?: string[]
}

export interface CourtWithSlotsDto {
  stadiumId: number
  stadiumName: string
  description?: string | null
  pricePerHour: number
  parentStadiumId: number
  stadiumStatus: StadiumStatus
  /** True nếu bị chặn đặt HÔM NAY do bảo trì (kể cả cascade từ Facility/Complex cha) — dù stadiumStatus vẫn AVAILABLE. */
  underMaintenanceToday?: boolean
  imageUrls: string[]
  timeSlots?: TimeSlotDto[]
}

export interface PageResponse<T> {
  content: T[]
  totalPages: number
  totalElements: number
  size: number
  number: number
}

export interface ComplexSearchParams {
  keyword?: string
  sportTypeId?: number
  address?: string
  province?: string
  district?: string
  userLat?: number
  userLng?: number
  radiusInKm?: number
  minPrice?: number
  maxPrice?: number
  page?: number
  size?: number
  amenityIds?: number[]
  targetDate?: string       // "yyyy-MM-dd"
  startTime?: string        // "HH:mm:ss"
  endTime?: string          // "HH:mm:ss"
}
