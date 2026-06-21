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

export interface CreateReviewPayload {
  ratingScore: number
  comment: string
}

/**
 * UC-CUS-08: Sửa đánh giá đã có.
 */
export async function updateReview(reviewId: number, payload: CreateReviewPayload): Promise<ReviewResponse> {
  const { data } = await api.put<ReviewResponse>(`/reviews/${reviewId}`, payload)
  return data
}
