'use client'

import { useState, useEffect } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { stadiumService } from '@/lib/services/stadium'
import { SportType } from '@/types/stadium'
import { toast } from 'sonner'

interface QuickCreateFacilityDialogProps {
  isOpen: boolean
  onClose: () => void
  complexId: number | null
  onSuccess: () => void
}

export function QuickCreateFacilityDialog({ isOpen, onClose, complexId, onSuccess }: QuickCreateFacilityDialogProps) {
  const [stadiumName, setStadiumName] = useState('')
  const [description, setDescription] = useState('')
  const [sportTypeId, setSportTypeId] = useState<number | null>(null)
  const [openTime, setOpenTime] = useState('06:00')
  const [closeTime, setCloseTime] = useState('22:00')
  const [sportTypes, setSportTypes] = useState<SportType[]>([])
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (isOpen) {
      stadiumService.getSportTypes()
        .then(setSportTypes)
        .catch(() => toast.error('Không thể tải danh sách môn thể thao'))
    }
  }, [isOpen])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!complexId) return
    if (!stadiumName.trim()) {
      toast.error('Vui lòng nhập tên khu sân')
      return
    }
    if (!sportTypeId) {
      toast.error('Vui lòng chọn môn thể thao')
      return
    }

    setSubmitting(true)
    try {
      await stadiumService.createFacility({
        complexId,
        stadiumName: stadiumName.trim(),
        description: description.trim() || undefined,
        sportTypeId,
        openTime: `${openTime}:00`,
        closeTime: `${closeTime}:00`,
      })
      toast.success('Đã tạo khu sân thành công!')
      // Reset form
      setStadiumName('')
      setDescription('')
      setSportTypeId(null)
      setOpenTime('06:00')
      setCloseTime('22:00')
      onSuccess()
      onClose()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Đã có lỗi xảy ra khi tạo khu sân'
      toast.error(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[480px]">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Thêm Khu Sân Mới (L2)</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 py-2">
          <div className="space-y-2">
            <Label htmlFor="facility-name">Tên khu sân <span className="text-red-500">*</span></Label>
            <Input
              id="facility-name"
              placeholder="Ví dụ: Khu Sân Bóng Đá Kỳ Hòa"
              value={stadiumName}
              onChange={(e) => setStadiumName(e.target.value)}
              disabled={submitting}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="sport-type">Môn thể thao <span className="text-red-500">*</span></Label>
            <Select
              disabled={submitting}
              onValueChange={(val) => setSportTypeId(Number(val))}
              value={sportTypeId ? String(sportTypeId) : undefined}
            >
              <SelectTrigger>
                <SelectValue placeholder="Chọn môn thể thao" />
              </SelectTrigger>
              <SelectContent>
                {sportTypes.map((st) => (
                  <SelectItem key={st.sportTypeId} value={String(st.sportTypeId)}>
                    {st.sportName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="open-time">Giờ mở cửa</Label>
              <Input
                id="open-time"
                type="time"
                value={openTime}
                onChange={(e) => setOpenTime(e.target.value)}
                disabled={submitting}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="close-time">Giờ đóng cửa</Label>
              <Input
                id="close-time"
                type="time"
                value={closeTime}
                onChange={(e) => setCloseTime(e.target.value)}
                disabled={submitting}
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="facility-desc">Mô tả chi tiết</Label>
            <Textarea
              id="facility-desc"
              placeholder="Nhập thông tin giới thiệu, các lưu ý, quy định của khu sân..."
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
              {submitting ? 'Đang tạo...' : 'Tạo Khu Sân'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
