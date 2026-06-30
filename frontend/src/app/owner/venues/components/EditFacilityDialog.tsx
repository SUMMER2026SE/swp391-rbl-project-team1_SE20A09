'use client'

import { useEffect, useState } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { stadiumService } from '@/lib/services/stadium'
import { StadiumResponse, SportType } from '@/types/stadium'
import { toast } from 'sonner'

interface EditFacilityDialogProps {
  isOpen: boolean
  onClose: () => void
  facility: StadiumResponse | null
  complexSportTypeIds?: number[]
  onSuccess: () => void
}

export function EditFacilityDialog({
  isOpen,
  onClose,
  facility,
  complexSportTypeIds,
  onSuccess
}: EditFacilityDialogProps) {
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
        .then((allSports) => {
          if (complexSportTypeIds && complexSportTypeIds.length > 0) {
            setSportTypes(allSports.filter(s => complexSportTypeIds.includes(s.sportTypeId)))
          } else {
            setSportTypes(allSports)
          }
        })
        .catch(() => toast.error('Không thể tải danh sách môn thể thao'))
    }
  }, [isOpen, complexSportTypeIds])

  useEffect(() => {
    if (isOpen && facility) {
      setStadiumName(facility.stadiumName)
      setDescription(facility.description || '')
      setSportTypeId(facility.sportTypeId || null)
      setOpenTime(facility.openTime.substring(0, 5))
      setCloseTime(facility.closeTime.substring(0, 5))
    }
  }, [isOpen, facility])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!facility) return
    if (!stadiumName.trim()) {
      toast.error('Vui lòng nhập tên khu sân')
      return
    }
    if (!sportTypeId) {
      toast.error('Vui lòng chọn môn thể thao')
      return
    }
    if (openTime >= closeTime) {
      toast.error('Giờ mở cửa phải trước giờ đóng cửa')
      return
    }

    setSubmitting(true)
    try {
      await stadiumService.updateStadium(facility.stadiumId, {
        stadiumName: stadiumName.trim(),
        address: facility.address || 'Address',
        sportTypeId: sportTypeId,
        pricePerHour: facility.pricePerHour || 0,
        description: description.trim() || undefined,
        openTime: `${openTime}:00`,
        closeTime: `${closeTime}:00`,
        latitude: facility.latitude || 16.0544,
        longitude: facility.longitude || 108.2022,
        imageUrls: facility.imageUrls || [],
      })

      toast.success('Cập nhật khu sân thành công!')
      onSuccess()
      onClose()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Đã xảy ra lỗi khi cập nhật khu sân'
      toast.error(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[480px]">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Chỉnh sửa Khu sân (L2)</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 py-2">
          
          <div className="space-y-2">
            <Label htmlFor="edit-fac-name">Tên khu sân *</Label>
            <Input
              id="edit-fac-name"
              value={stadiumName}
              onChange={(e) => setStadiumName(e.target.value)}
              disabled={submitting}
              placeholder="VD: Sân bóng đá 7 người"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="edit-fac-sport">Môn thể thao *</Label>
            <Select
              disabled={submitting}
              onValueChange={(val) => setSportTypeId(Number(val))}
              value={sportTypeId ? String(sportTypeId) : undefined}
            >
              <SelectTrigger id="edit-fac-sport">
                <SelectValue placeholder="Chọn môn thể thao" />
              </SelectTrigger>
              <SelectContent>
                {sportTypes.map((type) => (
                  <SelectItem key={type.sportTypeId} value={String(type.sportTypeId)}>
                    {type.sportName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="edit-fac-open">Giờ mở cửa *</Label>
              <Input
                id="edit-fac-open"
                type="time"
                value={openTime}
                onChange={(e) => setOpenTime(e.target.value)}
                disabled={submitting}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-fac-close">Giờ đóng cửa *</Label>
              <Input
                id="edit-fac-close"
                type="time"
                value={closeTime}
                onChange={(e) => setCloseTime(e.target.value)}
                disabled={submitting}
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="edit-fac-desc">Mô tả chi tiết</Label>
            <Textarea
              id="edit-fac-desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={submitting}
              placeholder="Nhập thông tin giới thiệu khu sân..."
              rows={3}
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
