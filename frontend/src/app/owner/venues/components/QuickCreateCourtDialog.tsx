'use client'

import { useState } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { stadiumService } from '@/lib/services/stadium'
import { toast } from 'sonner'

interface QuickCreateCourtDialogProps {
  isOpen: boolean
  onClose: () => void
  parentStadiumId: number | null
  onSuccess: () => void
}

export function QuickCreateCourtDialog({ isOpen, onClose, parentStadiumId, onSuccess }: QuickCreateCourtDialogProps) {
  const [stadiumName, setStadiumName] = useState('')
  const [description, setDescription] = useState('')
  const [pricePerHour, setPricePerHour] = useState<number>(150000)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!parentStadiumId) return
    if (!stadiumName.trim()) {
      toast.error('Vui lòng nhập tên sân lẻ')
      return
    }
    if (pricePerHour < 0) {
      toast.error('Giá thuê mỗi giờ không được âm')
      return
    }

    setSubmitting(true)
    try {
      await stadiumService.createCourt({
        parentStadiumId,
        stadiumName: stadiumName.trim(),
        description: description.trim() || undefined,
        pricePerHour,
      })
      toast.success('Đã tạo sân lẻ thành công!')
      // Reset form
      setStadiumName('')
      setDescription('')
      setPricePerHour(150000)
      onSuccess()
      onClose()
    } catch (err: any) {
      toast.error(err.message || 'Đã có lỗi xảy ra khi tạo sân lẻ')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[480px]">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Thêm Sân Lẻ Mới (L3)</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 py-2">
          <div className="space-y-2">
            <Label htmlFor="court-name">Tên sân lẻ <span className="text-red-500">*</span></Label>
            <Input
              id="court-name"
              placeholder="Ví dụ: Sân số 1 (Sân 5 người)"
              value={stadiumName}
              onChange={(e) => setStadiumName(e.target.value)}
              disabled={submitting}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="price-per-hour">Giá thuê mỗi giờ (VNĐ) <span className="text-red-500">*</span></Label>
            <Input
              id="price-per-hour"
              type="number"
              min="0"
              step="10000"
              value={pricePerHour}
              onChange={(e) => setPricePerHour(Number(e.target.value))}
              disabled={submitting}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="court-desc">Mô tả sân</Label>
            <Textarea
              id="court-desc"
              placeholder="Nhập thông tin mô tả chi tiết của sân con (ví dụ: loại cỏ, kích thước...)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={submitting}
              rows={3}
            />
          </div>

          <DialogFooter className="pt-4">
            <Button type="button" variant="outline" onClick={onClose} disabled={submitting}>
              Hủy
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Đang tạo...' : 'Tạo Sân Lẻ'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
