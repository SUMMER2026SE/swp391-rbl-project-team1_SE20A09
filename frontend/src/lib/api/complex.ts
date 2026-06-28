import api from '../api'
import type {
  StadiumComplexDto,
  FacilityDto,
  CourtWithSlotsDto,
  PageResponse,
  ComplexSearchParams,
} from '@/types/complex'

/**
 * Fetch public complex detail (no auth required).
 * Uses ISR revalidation on the server side.
 */
export async function getComplexDetail(id: number): Promise<StadiumComplexDto> {
  const res = await api.get<StadiumComplexDto>(`/public/complexes/${id}`)
  return res.data
}

/**
 * Fetch courts under a complex with optional date for slot availability.
 * Used client-side with React Query for real-time slot status.
 */
export async function getComplexCourts(
  complexId: number,
  date?: string
): Promise<CourtWithSlotsDto[]> {
  const res = await api.get<CourtWithSlotsDto[]>(
    `/public/complexes/${complexId}/courts`,
    { params: date ? { date } : undefined }
  )
  return res.data
}

/**
 * Fetch facilities (L2) grouped under a complex.
 */
export async function getComplexFacilities(complexId: number): Promise<FacilityDto[]> {
  const res = await api.get<FacilityDto[]>(`/public/complexes/${complexId}/facilities`)
  return res.data
}

/**
 * Search complexes with full type safety — no any.
 */
export async function searchComplexes(
  params: ComplexSearchParams
): Promise<PageResponse<StadiumComplexDto>> {
  const res = await api.get<PageResponse<StadiumComplexDto>>(
    '/public/complexes',
    { params }
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
    const res = await api.get<{ complexId: number | null; stadiumId: number }>(
      `/public/stadiums/${courtId}/complex-ref`
    )
    return res.data
  } catch {
    return null
  }
}
