'use client'

import { useEffect, useState } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { stadiumService } from '@/lib/services/stadium'
import { TimeSlot } from '@/types/timeSlot'
import { toast } from 'sonner'
import { createOrUpdateException } from '@/lib/api/timeSlot'

interface EditTimeSlotDialogProps {
  isOpen: boolean
  onClose: () => void
  slot: TimeSlot | null
  onSuccess: () => void
  date?: string
}

export function EditTimeSlotDialog({ isOpen, onClose, slot, onSuccess, date }: EditTimeSlotDialogProps) {
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [pricePerSlot, setPricePerSlot] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (isOpen && slot) {
      // slice to HH:mm format
      setStartTime(slot.startTime.substring(0, 5))
      setEndTime(slot.endTime.substring(0, 5))
      setPricePerSlot(String(slot.pricePerSlot))
    }
  }, [isOpen, slot])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!slot) return
    if (!startTime || !endTime || !pricePerSlot) {
      toast.error('Vui lòng nhập đầy đủ thông tin')
      return
    }

    setSubmitting(true)
    try {
      if (date) {
        await createOrUpdateException(slot.slotId, date, {
          priceOverride: parseFloat(pricePerSlot),
          startTimeOverride: startTime.length === 5 ? `${startTime}:00` : startTime,
          endTimeOverride: endTime.length === 5 ? `${endTime}:00` : endTime
        })
        toast.success('Đã cập nhật ngoại lệ cho ngày ' + date)
      } else {
        await stadiumService.updateTimeSlot(slot.slotId, {
          startTime: `${startTime}:00`,
          endTime: `${endTime}:00`,
          pricePerSlot: parseFloat(pricePerSlot)
        })
        toast.success('Đã cập nhật khung giờ thành công!')
      }
      onSuccess()
      onClose()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Đã xảy ra lỗi khi cập nhật'
      toast.error(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[400px]">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Chỉnh sửa Khung giờ</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 py-2">
          <div className="space-y-2">
            <Label htmlFor="edit-start-time">Giờ bắt đầu</Label>
            <Input
              id="edit-start-time"
              type="time"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              disabled={submitting}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="edit-end-time">Giờ kết thúc</Label>
            <Input
              id="edit-end-time"
              type="time"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              disabled={submitting}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="edit-price-slot">Giá thuê (VNĐ)</Label>
            <Input
              id="edit-price-slot"
              type="number"
              value={pricePerSlot}
              onChange={(e) => setPricePerSlot(e.target.value)}
              disabled={submitting}
              placeholder="VD: 150000"
            />
          </div>

          <DialogFooter className="pt-4">
            <Button type="button" variant="outline" onClick={onClose} disabled={submitting}>
              Hủy
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Đang lưu...' : 'Lưu thay đổi'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
