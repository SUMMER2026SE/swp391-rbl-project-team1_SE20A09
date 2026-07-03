'use client'

import { useState, useEffect } from 'react'
import { useSession } from 'next-auth/react'
import { toast } from 'sonner'
import { IconStar, IconSend, IconLoader2, IconCheck } from '@tabler/icons-react'
import { getEligibleBookings, createReview, type EligibleBooking } from '@/lib/services/review-api'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

interface WriteReviewFormProps {
  stadiumId: number
  onReviewCreated?: () => void
}

/**
 * UC-CUS-07: Form viết đánh giá sân.
 * Chỉ hiện khi customer đã có booking COMPLETED chưa review.
 */
export default function WriteReviewForm({ stadiumId, onReviewCreated }: WriteReviewFormProps) {
  const { data: session } = useSession()
  const [eligibleBookings, setEligibleBookings] = useState<EligibleBooking[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [submitted, setSubmitted] = useState(false)

  // Form state
  const [selectedBookingId, setSelectedBookingId] = useState<number | null>(null)
  const [rating, setRating] = useState(0)
  const [hoverRating, setHoverRating] = useState(0)
  const [comment, setComment] = useState('')

  // Fetch eligible bookings when component mounts
  useEffect(() => {
    if (!session?.user) {
      setLoading(false)
      return
    }

    const fetchEligible = async () => {
      try {
        const bookings = await getEligibleBookings(stadiumId)
        setEligibleBookings(bookings)
        if (bookings.length > 0) {
          setSelectedBookingId(bookings[0].bookingId)
        }
      } catch {
        // Not eligible or not logged in — silently hide form
      } finally {
        setLoading(false)
      }
    }

    fetchEligible()
  }, [stadiumId, session])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!selectedBookingId) {
      toast.error('Vui lòng chọn lần đặt sân để đánh giá.')
      return
    }

    if (rating < 1 || rating > 5) {
      toast.error('Vui lòng chọn số sao đánh giá (1-5).')
      return
    }

    if (!comment.trim()) {
      toast.error('Vui lòng nhập nhận xét của bạn.')
      return
    }

    try {
      setSubmitting(true)
      await createReview(selectedBookingId, {
        ratingScore: rating,
        comment: comment.trim(),
      })
      toast.success('Đánh giá của bạn đã được gửi thành công! 🎉')
      setSubmitted(true)
      setRating(0)
      setComment('')
      onReviewCreated?.()
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Gửi đánh giá thất bại. Vui lòng thử lại.')
    } finally {
      setSubmitting(false)
    }
  }

  // Don't render if not logged in, loading, or no eligible bookings
  if (!session?.user || loading || eligibleBookings.length === 0) {
    return null
  }

  // Show success state after submit
  if (submitted) {
    return (
      <div className="bg-[#e8f7ee] border-[0.5px] border-[#9edbb6] rounded-[12px] p-5 flex flex-col items-center gap-2 text-center">
        <div className="w-10 h-10 rounded-full bg-[#1a8a4a] flex items-center justify-center">
          <IconCheck className="w-5 h-5 text-white" />
        </div>
        <p className="text-[14px] font-medium text-[#0d5c2e]">
          Cảm ơn bạn đã đánh giá!
        </p>
        <p className="text-[12px] text-[#1a8a4a]/70">
          Đánh giá của bạn giúp sân cải thiện chất lượng dịch vụ.
        </p>
      </div>
    )
  }

  const displayRating = hoverRating || rating

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white border-[0.5px] border-[#1a8a4a]/20 rounded-[12px] p-5 flex flex-col gap-4 shadow-sm"
    >
      <div>
        <h3 className="text-[14px] font-medium text-gray-800 mb-1">
          ✍️ Viết đánh giá
        </h3>
        <p className="text-[12px] text-gray-400">
          Chia sẻ trải nghiệm của bạn về sân này
        </p>
      </div>

      {/* Booking selector — only show if multiple eligible bookings */}
      {eligibleBookings.length > 1 && (
        <div>
          <Label className="block text-[11px] font-medium text-gray-500 uppercase tracking-wide mb-1.5">
            Chọn lần đặt sân
          </Label>
          <Select
            value={String(selectedBookingId ?? '')}
            onValueChange={(value) => setSelectedBookingId(Number(value))}
          >
            <SelectTrigger className="w-full text-[13px] bg-gray-50">
              <SelectValue placeholder="Chọn lần đặt sân..." />
            </SelectTrigger>
            <SelectContent>
              {eligibleBookings.map((b) => (
                <SelectItem key={b.bookingId} value={String(b.bookingId)}>
                  Ngày {new Date(b.reservationDate).toLocaleDateString('vi-VN')} • {b.slotStartTime?.slice(0, 5)} - {b.slotEndTime?.slice(0, 5)}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      )}

      {/* Star rating */}
      <div>
        <Label className="block text-[11px] font-medium text-gray-500 uppercase tracking-wide mb-2">
          Đánh giá sao
        </Label>
        <div className="flex items-center gap-1">
          {[1, 2, 3, 4, 5].map((star) => (
            <button
              key={star}
              type="button"
              onClick={() => setRating(star)}
              onMouseEnter={() => setHoverRating(star)}
              onMouseLeave={() => setHoverRating(0)}
              className="p-0.5 transition-transform hover:scale-110 active:scale-95"
            >
              <IconStar
                className={`w-7 h-7 transition-colors ${
                  star <= displayRating
                    ? 'fill-[#f0a500] text-[#f0a500]'
                    : 'text-gray-300'
                }`}
              />
            </button>
          ))}
          {displayRating > 0 && (
            <span className="ml-2 text-[13px] font-medium text-[#7a5800]">
              {['', 'Rất tệ', 'Tệ', 'Bình thường', 'Tốt', 'Tuyệt vời'][displayRating]}
            </span>
          )}
        </div>
      </div>

      {/* Comment */}
      <div>
        <Label className="block text-[11px] font-medium text-gray-500 uppercase tracking-wide mb-1.5">
          Nhận xét
        </Label>
        <Textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="Sân sạch sẽ, mặt cỏ tốt, nhân viên phục vụ nhiệt tình..."
          rows={3}
          maxLength={500}
          className="text-[13px] bg-gray-50 resize-none placeholder:text-gray-300"
        />
        <div className="flex justify-end mt-1">
          <span className="text-[11px] text-gray-300">{comment.length}/500</span>
        </div>
      </div>

      {/* Submit */}
      <Button
        type="submit"
        disabled={submitting || rating === 0 || !comment.trim()}
        className="w-full bg-[#1a8a4a] hover:bg-[#157a3e] text-white"
      >
        {submitting ? (
          <>
            <IconLoader2 className="w-4 h-4 animate-spin mr-2" />
            Đang gửi...
          </>
        ) : (
          <>
            <IconSend className="w-4 h-4 mr-2" />
            Gửi đánh giá
          </>
        )}
      </Button>
    </form>
  )
}
