'use client'

import { useEffect, useState } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import { Badge } from '@/components/ui/badge'
import { Upload, X, Loader2 } from 'lucide-react'
import { stadiumService } from '@/lib/services/stadium'
import { getAmenities, Amenity } from '@/lib/api/stadium'
import { uploadStadiumImage } from '@/lib/api'
import { ComplexResponse, SportType } from '@/types/stadium'
import { toast } from 'sonner'
import { AddressPicker } from '@/components/AddressPicker'

interface EditComplexDialogProps {
  isOpen: boolean
  onClose: () => void
  complex: ComplexResponse | null
  onSuccess: () => void
}

export function EditComplexDialog({ isOpen, onClose, complex, onSuccess }: EditComplexDialogProps) {
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [description, setDescription] = useState('')
  const [address, setAddress] = useState('')
  const [latitude, setLatitude] = useState(16.0544)
  const [longitude, setLongitude] = useState(108.2022)
  const [uploadedPhotos, setUploadedPhotos] = useState<string[]>([])
  const [selectedSportIds, setSelectedSportIds] = useState<number[]>([])
  const [selectedAmenityIds, setSelectedAmenityIds] = useState<number[]>([])

  const [sportTypes, setSportTypes] = useState<SportType[]>([])
  const [amenities, setAmenities] = useState<Amenity[]>([])
  const [isUploading, setIsUploading] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (isOpen) {
      Promise.all([
        stadiumService.getSportTypes(),
        getAmenities()
      ])
        .then(([sports, amens]) => {
          setSportTypes(sports)
          setAmenities(amens)
        })
        .catch(() => toast.error('Không thể tải danh sách môn thể thao và tiện ích'))
    }
  }, [isOpen])

  useEffect(() => {
    if (isOpen && complex) {
      setName(complex.name)
      setPhone(complex.phone || '')
      setDescription(complex.description || '')
      setAddress(complex.address)
      setLatitude(complex.latitude || 16.0544)
      setLongitude(complex.longitude || 108.2022)
      setUploadedPhotos(complex.imageUrls || [])
      setSelectedSportIds(complex.sportTypeIds || [])
      setSelectedAmenityIds(complex.amenityIds || [])
    }
  }, [isOpen, complex])

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
    if (!complex) return
    if (!name.trim()) {
      toast.error('Vui lòng nhập tên tổ hợp')
      return
    }
    if (!address.trim()) {
      toast.error('Vui lòng nhập địa chỉ')
      return
    }
    if (selectedSportIds.length === 0) {
      toast.error('Vui lòng chọn ít nhất 1 môn thể thao')
      return
    }
    if (uploadedPhotos.length === 0) {
      toast.error('Vui lòng tải lên ít nhất 1 ảnh')
      return
    }

    setSubmitting(true)
    try {
      await stadiumService.updateComplex(complex.complexId, {
        name: name.trim(),
        address: address.trim(),
        phone: phone.trim() || undefined,
        latitude,
        longitude,
        description: description.trim() || undefined,
        sportTypeIds: selectedSportIds,
        amenityIds: selectedAmenityIds,
        coverImageUrl: uploadedPhotos[0],
        imageUrls: uploadedPhotos,
      })
      toast.success('Đã cập nhật tổ hợp thành công! Trạng thái đang chuyển về Chờ duyệt để Admin kiểm duyệt lại.')
      onSuccess()
      onClose()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Đã xảy ra lỗi khi cập nhật tổ hợp'
      toast.error(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[650px] max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">Chỉnh sửa thông tin Tổ hợp (L1)</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-6 py-2">
          
          {/* Warning Alert */}
          <div className="bg-amber-50 border border-amber-200 rounded-lg p-3.5 text-xs text-amber-800 flex items-start gap-2.5">
            <span className="text-base shrink-0">⚠️</span>
            <div>
              <strong className="font-bold block mb-0.5">Lưu ý về kiểm duyệt địa điểm:</strong>
              Việc thay đổi <span className="font-semibold underline">Địa chỉ</span> hoặc <span className="font-semibold underline">Vị trí bản đồ</span> sẽ đưa Tổ hợp về trạng thái <strong>Chờ duyệt (PENDING)</strong> để Admin xác thực lại địa điểm thực tế. Các thay đổi khác (Tên, Ảnh, SĐT, Môn thể thao, Tiện ích) sẽ được cập nhật trực tiếp mà không bị gián đoạn hoạt động.
            </div>
          </div>

          {/* Tên & SĐT */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="edit-name">Tên tổ hợp <span className="text-red-500">*</span></Label>
              <Input
                id="edit-name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                disabled={submitting}
                placeholder="VD: Tổ hợp Kỳ Hòa"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-phone">Số điện thoại liên hệ</Label>
              <Input
                id="edit-phone"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                disabled={submitting}
                placeholder="VD: 0901234567"
              />
            </div>
          </div>

          {/* Mô tả */}
          <div className="space-y-2">
            <Label htmlFor="edit-desc">Mô tả</Label>
            <Textarea
              id="edit-desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={submitting}
              placeholder="Nhập thông tin giới thiệu tổ hợp..."
              rows={3}
            />
          </div>

          {/* Vị trí */}
          <div className="space-y-2">
            <AddressPicker
              initialAddress={address}
              initialLat={latitude}
              initialLng={longitude}
              onAddressChange={(data) => {
                setAddress(data.addressText)
                setLatitude(data.lat)
                setLongitude(data.lng)
              }}
            />
          </div>

          {/* Album ảnh */}
          <div className="space-y-2">
            <Label>Hình ảnh tổ hợp <span className="text-red-500">*</span></Label>
            <div className="grid grid-cols-3 gap-3">
              {uploadedPhotos.map((photo, idx) => (
                <div key={idx} className="relative aspect-video">
                  <img
                    src={photo}
                    alt={`Photo ${idx + 1}`}
                    className="w-full h-full object-cover rounded-lg border"
                  />
                  {idx === 0 && (
                    <Badge className="absolute top-1 left-1 text-[9px] px-1 py-0 h-4">Ảnh bìa</Badge>
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

          {/* Môn thể thao (Checkboxes) */}
          <div className="space-y-3">
            <Label className="text-sm font-bold">Môn thể thao hỗ trợ <span className="text-red-500">*</span></Label>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {sportTypes.map((type) => {
                const isChecked = selectedSportIds.includes(type.sportTypeId)
                return (
                  <div key={type.sportTypeId} className="flex items-center space-x-2 border rounded-lg p-2 bg-white hover:bg-slate-50">
                    <Checkbox
                      id={`edit-sport-${type.sportTypeId}`}
                      checked={isChecked}
                      onCheckedChange={(checked) => {
                        if (checked) {
                          setSelectedSportIds([...selectedSportIds, type.sportTypeId])
                        } else {
                          setSelectedSportIds(selectedSportIds.filter(id => id !== type.sportTypeId))
                        }
                      }}
                    />
                    <Label htmlFor={`edit-sport-${type.sportTypeId}`} className="text-xs font-semibold cursor-pointer select-none">
                      {type.sportName}
                    </Label>
                  </div>
                )
              })}
            </div>
          </div>

          {/* Tiện nghi (Checkboxes) */}
          <div className="space-y-3">
            <Label className="text-sm font-bold">Tiện ích sẵn có (Amenities)</Label>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {amenities.map((amenity) => {
                const isChecked = selectedAmenityIds.includes(amenity.amenityId)
                return (
                  <div key={amenity.amenityId} className="flex items-center space-x-2 border rounded-lg p-2 bg-white hover:bg-slate-50">
                    <Checkbox
                      id={`edit-amenity-${amenity.amenityId}`}
                      checked={isChecked}
                      onCheckedChange={(checked) => {
                        if (checked) {
                          setSelectedAmenityIds([...selectedAmenityIds, amenity.amenityId])
                        } else {
                          setSelectedAmenityIds(selectedAmenityIds.filter(id => id !== amenity.amenityId))
                        }
                      }}
                    />
                    <Label htmlFor={`edit-amenity-${amenity.amenityId}`} className="text-xs font-semibold cursor-pointer select-none">
                      {amenity.name}
                    </Label>
                  </div>
                )
              })}
            </div>
          </div>

          <DialogFooter className="pt-4 border-t">
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
