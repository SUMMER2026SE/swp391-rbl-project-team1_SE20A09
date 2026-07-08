'use client'

import { useState } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { stadiumService } from '@/lib/services/stadium'
import { toast } from 'sonner'
import { uploadStadiumImage } from '@/lib/api'
import { X, Upload, Loader2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { FootballFieldType, StadiumResponse } from '@/types/stadium'
import { FOOTBALL_FIELD_TYPE_OPTIONS } from '@/lib/constants/football-field-types'

interface QuickCreateCourtDialogProps {
  isOpen: boolean
  onClose: () => void
  parentFacility: StadiumResponse | null
  onSuccess: () => void
}

export function QuickCreateCourtDialog({ isOpen, onClose, parentFacility, onSuccess }: QuickCreateCourtDialogProps) {
  const [stadiumName, setStadiumName] = useState('')
  const [description, setDescription] = useState('')
  const [pricePerHour, setPricePerHour] = useState<number>(150000)
  const [uploadedPhotos, setUploadedPhotos] = useState<string[]>([])
  const [footballFieldType, setFootballFieldType] = useState<FootballFieldType | ''>('')
  const [isUploading, setIsUploading] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (!files || files.length === 0) return
    if (uploadedPhotos.length + files.length > 10) {
      toast.error('Chỉ được tải lên tối đa 10 ảnh')
      e.target.value = ''
      return
    }
    setIsUploading(true)
    let successCount = 0
    for (const file of Array.from(files)) {
      try {
        const result = await uploadStadiumImage(file)
        setUploadedPhotos(prev => [...prev, result.url])
        successCount++
      } catch {
        toast.error(`Không thể tải lên ảnh "${file.name}"`)
      }
    }
    if (successCount > 0) toast.success(`Đã tải lên ${successCount} ảnh`)
    setIsUploading(false)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!parentFacility?.stadiumId) return
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
        parentStadiumId: parentFacility.stadiumId,
        stadiumName: stadiumName.trim(),
        description: description.trim() || undefined,
        pricePerHour,
        imageUrls: uploadedPhotos,
        footballFieldType: footballFieldType === '' ? undefined : footballFieldType,
      })
      toast.success('Đã tạo sân lẻ thành công!')
      // Reset form
      setStadiumName('')
      setDescription('')
      setPricePerHour(150000)
      setUploadedPhotos([])
      setFootballFieldType('')
      onSuccess()
      onClose()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Đã có lỗi xảy ra khi tạo sân lẻ'
      toast.error(msg)
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

          {parentFacility?.isFootballType ? (
            <div className="space-y-2">
              <Label htmlFor="court-type">Loại sân bóng đá (tuỳ chọn)</Label>
              <Select 
                value={footballFieldType} 
                onValueChange={(val) => setFootballFieldType(val as FootballFieldType)}
                disabled={submitting}
              >
                <SelectTrigger id="court-type">
                  <SelectValue placeholder="Chọn loại sân (nếu là sân bóng đá)" />
                </SelectTrigger>
                <SelectContent>
                  {FOOTBALL_FIELD_TYPE_OPTIONS.map(opt => (
                    <SelectItem key={opt.value} value={opt.value}>{opt.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          ) : null}

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

          {/* Album ảnh */}
          <div className="space-y-2">
            <Label>Hình ảnh sân lẻ</Label>
            <div className="grid grid-cols-3 gap-3">
              {uploadedPhotos.map((photo, idx) => (
                <div key={idx} className="relative aspect-video">
                  <img
                    src={photo}
                    alt={`Photo ${idx + 1}`}
                    className="w-full h-full object-cover rounded-lg border"
                  />
                  {idx === 0 && (
                    <Badge className="absolute top-1 left-1 text-[9px] px-1 py-0 h-4 bg-primary text-primary-foreground hover:bg-primary">Ảnh bìa</Badge>
                  )}
                  <button
                    type="button"
                    className="absolute top-1 right-1 bg-destructive text-destructive-foreground rounded-full p-0.5 shadow-sm"
                    onClick={() => setUploadedPhotos(uploadedPhotos.filter((_, i) => i !== idx))}
                  >
                    <X className="h-3 w-3" />
                  </button>
                </div>
              ))}
              <label className="aspect-video border-2 border-dashed rounded-lg flex flex-col items-center justify-center text-muted-foreground hover:border-primary hover:text-primary cursor-pointer transition-colors bg-muted/50">
                {isUploading ? (
                  <Loader2 className="h-6 w-6 animate-spin" />
                ) : (
                  <>
                     <Upload className="h-6 w-6 mb-1" />
                     <span className="text-[10px]">Tải ảnh</span>
                  </>
                )}
                <input
                  type="file"
                  multiple
                  accept="image/*"
                  className="hidden"
                  onChange={handlePhotoUpload}
                  disabled={isUploading}
                />
              </label>
            </div>
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
