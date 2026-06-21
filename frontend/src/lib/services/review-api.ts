import api from '@/lib/api'

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

export async function getEligibleBookings(stadiumId: number): Promise<EligibleBooking[]> {
  const { data } = await api.get<{ result: EligibleBooking[] }>(`/reviews/eligible/${stadiumId}`)
  return data.result || []
}

export async function createReview(bookingId: number, payload: CreateReviewPayload): Promise<ReviewResponse> {
  const { data } = await api.post<ReviewResponse>(`/reviews/bookings/${bookingId}/reviews`, payload)
  return data
}

export async function getMyReviews(page = 0, size = 10) {
  const { data } = await api.get('/reviews/me', { params: { page, size } })
  return data
}

export async function updateReview(reviewId: number, payload: CreateReviewPayload): Promise<ReviewResponse> {
  const { data } = await api.put<ReviewResponse>(`/reviews/${reviewId}`, payload)
  return data
}
