import api from '@/lib/api'

/**
 * UC-CUS-07: API functions cho Review Management.
 */

export interface ReviewResponse {
  reviewId: number
  bookingId: number
  stadiumId: number
  reviewerName: string
  ratingScore: number
  comment: string
  ownerResponse: string | null
  createdAt: string
}

export interface EligibleBooking {
  bookingId: number
  stadiumId: number
  stadiumName: string
  reservationDate: string
  slotStartTime: string
  slotEndTime: string
  bookingDate: string
}

export interface CreateReviewPayload {
  ratingScore: number
  comment: string
}

/**
 * Lấy danh sách booking COMPLETED chưa review cho sân cụ thể.
 * Trả về [] nếu không có → customer không đủ điều kiện review.
 */
export async function getEligibleBookings(stadiumId: number): Promise<EligibleBooking[]> {
  const { data } = await api.get<{ result: EligibleBooking[] }>(`/reviews/eligible/${stadiumId}`)
  return data.result || []
}

/**
 * Tạo review cho một booking đã hoàn thành.
 */
export async function createReview(bookingId: number, payload: CreateReviewPayload): Promise<ReviewResponse> {
  const { data } = await api.post<ReviewResponse>(`/reviews/bookings/${bookingId}/reviews`, payload)
  return data
}

/**
 * Lấy danh sách review của customer hiện tại.
 */
export async function getMyReviews(page = 0, size = 10) {
  const { data } = await api.get('/reviews/me', { params: { page, size } })
  return data
}
