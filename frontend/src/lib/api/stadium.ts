import api from '../api'

export interface Amenity {
  amenityId: number
  name: string
  icon: string
}

export interface StadiumResponse {
  stadiumId: number
  stadiumName: string
  description: string
  address: string
  pricePerHour: number
  capacity: number
  averageRating: number
  latitude?: number
  longitude?: number
  distanceInKm?: number
  sportTypeName: string
  firstImageUrl: string
  amenities: Amenity[]
}

export interface PageResponse<T> {
  content: T[]
  pageNo: number
  pageSize: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface StadiumSearchRequest {
  keyword?: string
  sportTypeId?: number
  address?: string
  minPrice?: number
  maxPrice?: number
  targetDate?: string // YYYY-MM-DD
  startTime?: string // HH:MM:SS
  endTime?: string // HH:MM:SS
  userLat?: number
  userLng?: number
  radiusInKm?: number
  amenityIds?: number[]
  page?: number
  size?: number
}

export async function searchStadiums(params: StadiumSearchRequest): Promise<PageResponse<StadiumResponse>> {
  // Convert array to comma-separated string for URL parameters if needed,
  // or let axios handle array serialization
  const res = await api.get<PageResponse<StadiumResponse>>('/public/stadiums', {
    params: {
      ...params,
      amenityIds: params.amenityIds?.join(','),
    }
  })
  return res.data
}

export async function getAmenities(): Promise<Amenity[]> {
  const res = await api.get<Amenity[]>('/public/amenities')
  return res.data
}

export async function getSportTypes(): Promise<{ sportTypeId: number, sportName: string }[]> {
  const res = await api.get<{ sportTypeId: number, sportName: string }[]>('/sport-types')
  return res.data
}
