'use client'

import { useState } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'
import { stadiumService } from '@/lib/services/stadium'
import { StadiumResponse, CreateTimeSlotRequest } from '@/types/stadium'
import { toast } from 'sonner'

interface BulkTimeSlotConfigDialogProps {
  isOpen: boolean
  onClose: () => void
  facilityId: number | null
  complexId: number | null
  courts: StadiumResponse[]
  onSuccess: () => void
}

export function BulkTimeSlotConfigDialog({
  isOpen,
  onClose,
  facilityId,
  complexId,
  courts,
  onSuccess
}: BulkTimeSlotConfigDialogProps) {
  const [openTime, setOpenTime] = useState('06:00')
  const [closeTime, setOpenTimeClose] = useState('22:00')
  const [duration, setDuration] = useState<number>(60)
  const [pricePerSlot, setPricePerSlot] = useState<number>(150000)
  const [selectedCourtIds, setSelectedCourtIds] = useState<number[]>([])
  const [applyToAll, setApplyToAll] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  const handleToggleCourt = (courtId: number) => {
    setSelectedCourtIds(prev => 
      prev.includes(courtId) ? prev.filter(id => id !== courtId) : [...prev, courtId]
    )
  }

  // Safe slot generation algorithm
  const generateSlots = (start: string, end: string, dur: number, price: number): CreateTimeSlotRequest[] => {
    const slots: CreateTimeSlotRequest[] = []
    const [startH, startM] = start.split(':').map(Number)
    const [endH, endM] = end.split(':').map(Number)
    
    let current = startH * 60 + startM
    const limit = endH * 60 + endM
    
    const pad = (n: number) => String(n).padStart(2, '0')

    while (current + dur <= limit) {
      const curH = Math.floor(current / 60)
      const curM = current % 60
      const nextH = Math.floor((current + dur) / 60)
      const nextM = (current + dur) % 60
      
      slots.push({
        startTime: `${pad(curH)}:${pad(curM)}:00`,
        endTime: `${pad(nextH)}:${pad(nextM)}:00`,
        pricePerSlot: price
      })
      current += dur
    }
    return slots
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!facilityId && !complexId) return
    if (!applyToAll && selectedCourtIds.length === 0) {
      toast.error('Vui lòng chọn ít nhất một sân con để áp dụng')
      return
    }

    const generated = generateSlots(openTime, closeTime, duration, pricePerSlot)
    if (generated.length === 0) {
      toast.error('Không thể sinh khung giờ nào. Vui lòng kiểm tra thời lượng và giờ mở/đóng.')
      return
    }

    setSubmitting(true)
    try {
      const payload = {
        courtIds: applyToAll ? undefined : selectedCourtIds,
        applyToAllCourts: applyToAll,
        slots: generated
      }

      if (facilityId) {
        await stadiumService.bulkCreateSlotsForFacility(facilityId, payload)
      } else if (complexId) {
        await stadiumService.bulkCreateSlotsForComplex(complexId, payload)
      }

      toast.success(`Đã tự động sinh và áp dụng ${generated.length} khung giờ hàng loạt thành công!`)
      setSelectedCourtIds([])
      setApplyToAll(true)
      onSuccess()
      onClose()
    } catch (err: any) {
      toast.error(err.message || 'Đã có lỗi xảy ra khi cấu hình giờ hàng loạt')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[520px]">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Cấu Hình Khung Giờ Hàng Loạt</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 py-2">
          
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="bulk-open-time">Giờ bắt đầu</Label>
              <Input
                id="bulk-open-time"
                type="time"
                value={openTime}
                onChange={(e) => setOpenTime(e.target.value)}
                disabled={submitting}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="bulk-close-time">Giờ kết thúc</Label>
              <Input
                id="bulk-close-time"
                type="time"
                value={closeTime}
                onChange={(e) => setOpenTimeClose(e.target.value)}
                disabled={submitting}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="bulk-duration">Thời lượng mỗi ca</Label>
              <Select
                disabled={submitting}
                onValueChange={(val) => setDuration(Number(val))}
                value={String(duration)}
              >
                <SelectTrigger id="bulk-duration">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="60">60 phút (1 giờ)</SelectItem>
                  <SelectItem value="90">90 phút (1.5 giờ)</SelectItem>
                  <SelectItem value="120">120 phút (2 giờ)</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="bulk-price">Giá mỗi ca (VNĐ)</Label>
              <Input
                id="bulk-price"
                type="number"
                min="0"
                step="10000"
                value={pricePerSlot}
                onChange={(e) => setPricePerSlot(Number(e.target.value))}
                disabled={submitting}
              />
            </div>
          </div>

          {/* Target Court Selection */}
          <div className="space-y-3 pt-2">
            <div className="flex items-center gap-2">
              <Checkbox
                id="apply-all"
                checked={applyToAll}
                onCheckedChange={(checked) => setApplyToAll(!!checked)}
                disabled={submitting}
              />
              <Label htmlFor="apply-all" className="font-bold cursor-pointer">
                Áp dụng cho tất cả sân lẻ trực thuộc
              </Label>
            </div>

            {!applyToAll && courts.length > 0 && (
              <div className="border rounded-xl p-3 bg-gray-50/50 dark:bg-muted/40 max-h-36 overflow-y-auto space-y-2">
                <Label className="text-xs text-muted-foreground block mb-1">Chọn danh sách sân con áp dụng:</Label>
                {courts.map(court => (
                  <div key={court.stadiumId} className="flex items-center gap-2">
                    <Checkbox
                      id={`court-${court.stadiumId}`}
                      checked={selectedCourtIds.includes(court.stadiumId)}
                      onCheckedChange={() => handleToggleCourt(court.stadiumId)}
                      disabled={submitting}
                    />
                    <Label htmlFor={`court-${court.stadiumId}`} className="text-sm cursor-pointer">
                      {court.stadiumName}
                    </Label>
                  </div>
                ))}
              </div>
            )}
          </div>

          <DialogFooter className="pt-4">
            <Button type="button" variant="outline" onClick={onClose} disabled={submitting}>
              Hủy
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Đang tạo...' : 'Áp Dụng Hàng Loạt'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
