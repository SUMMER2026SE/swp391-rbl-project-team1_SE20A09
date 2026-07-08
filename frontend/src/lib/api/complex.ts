import api, { publicApi } from '../api'
import type {
  StadiumComplexDto,
  FacilityDto,
  CourtWithSlotsDto,
  PageResponse,
  ComplexSearchParams,
} from '@/types/complex'
import type { ReviewDto, PageResponse as ReviewPageResponse } from './venue'

/**
 * Fetch public complex detail (no auth required).
 * Uses ISR revalidation on the server side.
 */
export async function getComplexDetail(id: number): Promise<StadiumComplexDto> {
  const res = await publicApi.get<StadiumComplexDto>(`/public/complexes/${id}`)
  return res.data
}

/**
 * Fetch all courts directly under a complex (L1 → L3, skipping facility).
 * Used when complex has no facility layer, or for a flat list overview.
 */
export async function getComplexCourts(
  complexId: number,
  date?: string
): Promise<CourtWithSlotsDto[]> {
  const res = await publicApi.get<CourtWithSlotsDto[]>(
    `/public/complexes/${complexId}/courts`,
    { params: date ? { date } : undefined }
  )
  return res.data
}

/**
 * Fetch courts under a specific facility (L2 → L3).
 * This is the correct function to use from the complex detail page
 * where the user has already selected a facility tab.
 */
export async function getFacilityCourts(
  facilityId: number,
  date?: string
): Promise<CourtWithSlotsDto[]> {
  const res = await publicApi.get<CourtWithSlotsDto[]>(
    `/public/facilities/${facilityId}/courts`,
    { params: date ? { date } : undefined }
  )
  return res.data
}

/**
 * Fetch facilities (L2) grouped under a complex.
 */
export async function getComplexFacilities(complexId: number): Promise<FacilityDto[]> {
  const res = await publicApi.get<FacilityDto[]>(`/public/complexes/${complexId}/facilities`)
  return res.data
}

/**
 * Search complexes with full type safety — no any.
 */
export async function searchComplexes(
  params: ComplexSearchParams
): Promise<PageResponse<StadiumComplexDto>> {
  const res = await publicApi.get<PageResponse<StadiumComplexDto>>(
    '/public/complexes',
    { params }
  )
  return res.data
}

/**
 * Fetch reviews for a complex, aggregated across all courts under it
 * (reviews are stored per-court — the backend merges them by complex_id).
 */
export async function getComplexReviews(
  complexId: number,
  page: number = 0,
  size: number = 5
): Promise<ReviewPageResponse<ReviewDto>> {
  const res = await publicApi.get<ReviewPageResponse<ReviewDto>>(
    `/public/complexes/${complexId}/reviews`,
    { params: { page, size } }
  )
  return res.data
}

/**
 * Fetch court detail with its parent complex info (for server-side redirect).
 * Returns null if not found.
 */
export async function getCourtWithComplex(
  courtId: number
): Promise<{ complexId: number | null; stadiumId: number } | null> {
  try {
    const res = await publicApi.get<{ complexId: number | null; stadiumId: number }>(
      `/public/stadiums/${courtId}/complex-ref`
    )
    return res.data
  } catch {

    return null
  }
}
