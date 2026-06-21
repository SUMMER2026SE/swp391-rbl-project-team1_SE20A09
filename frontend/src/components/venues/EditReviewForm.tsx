'use client'

import { useState } from 'react'
import { toast } from 'sonner'
import { IconStar, IconSend, IconLoader2, IconX } from '@tabler/icons-react'
import { updateReview } from '@/lib/services/review-api'

interface EditReviewFormProps {
  reviewId: number
  initialRating: number
  initialComment: string
  onCancel: () => void
  onSuccess: () => void
}

/**
 * UC-CUS-08: Form sửa đánh giá.
 */
export default function EditReviewForm({ reviewId, initialRating, initialComment, onCancel, onSuccess }: EditReviewFormProps) {
  const [rating, setRating] = useState(initialRating)
  const [hoverRating, setHoverRating] = useState(0)
  const [comment, setComment] = useState(initialComment)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

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
      await updateReview(reviewId, {
        ratingScore: rating,
        comment: comment.trim(),
      })
      toast.success('Cập nhật đánh giá thành công! 🎉')
      onSuccess()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Sửa đánh giá thất bại. Vui lòng thử lại.'
      toast.error(msg)
    } finally {
      setSubmitting(false)
    }
  }

  const displayRating = hoverRating || rating

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white border-[0.5px] border-[#1a8a4a]/20 rounded-[12px] p-4 flex flex-col gap-3 shadow-sm mt-2"
    >
      <div className="flex items-center justify-between">
        <h3 className="text-[13px] font-medium text-gray-800">
          ✏️ Sửa đánh giá
        </h3>
        <button type="button" onClick={onCancel} className="text-gray-400 hover:text-gray-600 transition-colors">
          <IconX className="w-4 h-4" />
        </button>
      </div>

      {/* Star rating */}
      <div>
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
                className={`w-6 h-6 transition-colors ${
                  star <= displayRating
                    ? 'fill-[#f0a500] text-[#f0a500]'
                    : 'text-gray-300'
                }`}
              />
            </button>
          ))}
          {displayRating > 0 && (
            <span className="ml-2 text-[12px] font-medium text-[#7a5800]">
              {['', 'Rất tệ', 'Tệ', 'Bình thường', 'Tốt', 'Tuyệt vời'][displayRating]}
            </span>
          )}
        </div>
      </div>

      {/* Comment */}
      <div>
        <textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="Sân sạch sẽ..."
          rows={3}
          maxLength={500}
          className="w-full border border-gray-200 rounded-[8px] px-3 py-2 text-[13px] text-gray-700 bg-gray-50 resize-none focus:outline-none focus:ring-2 focus:ring-[#1a8a4a]/30 focus:border-[#1a8a4a] placeholder:text-gray-300"
        />
        <div className="flex justify-end mt-1">
          <span className="text-[11px] text-gray-300">{comment.length}/500</span>
        </div>
      </div>

      {/* Submit */}
      <div className="flex justify-end gap-2">
        <button
          type="button"
          onClick={onCancel}
          disabled={submitting}
          className="px-3 py-1.5 rounded-[6px] text-[12px] font-medium text-gray-600 hover:bg-gray-100 transition-colors"
        >
          Hủy
        </button>
        <button
          type="submit"
          disabled={submitting || rating === 0 || !comment.trim() || (rating === initialRating && comment.trim() === initialComment.trim())}
          className="flex items-center gap-1.5 bg-[#1a8a4a] hover:bg-[#157a3e] disabled:bg-gray-300 disabled:cursor-not-allowed text-white font-medium text-[12px] px-4 py-1.5 rounded-[6px] transition-colors"
        >
          {submitting ? (
            <IconLoader2 className="w-3.5 h-3.5 animate-spin" />
          ) : (
            <IconSend className="w-3.5 h-3.5" />
          )}
          <span>Lưu</span>
        </button>
      </div>
    </form>
  )
}
