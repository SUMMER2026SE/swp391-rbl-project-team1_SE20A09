import api from '../api'
import { AxiosRequestConfig } from 'axios'

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
  averageRating: number
  totalReviews?: number
  latitude?: number
  longitude?: number
  distanceInKm?: number
  sportName: string
  isFootballType?: boolean
  firstImageUrl: string | null
  amenities: Amenity[]
  /** Tên khu phức hợp chứa sân — undefined/null nếu sân độc lập. */
  complexName?: string | null
}

export interface PageResponse<T> {
  content: T[]
  pageNumber: number
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

export async function getAmenities(config?: AxiosRequestConfig): Promise<Amenity[]> {
  const res = await api.get<Amenity[]>('/public/amenities', config)
  return res.data
}

export async function getSportTypes(config?: AxiosRequestConfig): Promise<{ sportTypeId: number, sportName: string }[]> {
  const res = await api.get<{ sportTypeId: number, sportName: string }[]>('/sport-types', config)
  return res.data
}
