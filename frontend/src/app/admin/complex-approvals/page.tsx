'use client'

import { useEffect, useState } from 'react'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { CheckCircle, XCircle, MapPin, Phone, Loader2, ImageOff, Calendar, Eye, Info } from 'lucide-react'
import { stadiumService } from '@/lib/services/stadium'
import { getAmenities, Amenity } from '@/lib/api/stadium'
import { ComplexResponse } from '@/types/stadium'
import { toast } from 'sonner'
import { format } from 'date-fns'

export default function ComplexApprovalPage() {
  const [complexes, setComplexes] = useState<ComplexResponse[]>([])
  const [amenities, setAmenities] = useState<Amenity[]>([])
  const [loading, setLoading] = useState(true)
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)
  const [activeTab, setActiveTab] = useState('PENDING')

  // Rejection state
  const [rejectingComplexId, setRejectingComplexId] = useState<number | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const [isSubmittingReject, setIsSubmittingReject] = useState(false)

  // Detail Modal state
  const [detailComplex, setDetailComplex] = useState<ComplexResponse | null>(null)

  const fetchComplexes = (status: string) => {
    setLoading(true)
    stadiumService.getAllComplexesAdmin(status)
      .then(setComplexes)
      .catch(() => toast.error('Không thể tải danh sách tổ hợp chờ duyệt'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    fetchComplexes(activeTab)
    getAmenities()
      .then(setAmenities)
      .catch(() => {})
  }, [activeTab])

  const handleApprove = async (complexId: number) => {
    setActionLoadingId(complexId)
    try {
      await stadiumService.approveComplex(complexId)
      toast.success('Đã duyệt tổ hợp thành công!')
      setDetailComplex(null) // close details if open
      fetchComplexes(activeTab)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể phê duyệt tổ hợp'
      toast.error(msg)
    } finally {
      setActionLoadingId(null)
    }
  }

  const handleRejectSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!rejectingComplexId) return
    if (!rejectReason.trim()) {
      toast.error('Vui lòng nhập lý do từ chối')
      return
    }

    setIsSubmittingReject(true)
    try {
      await stadiumService.rejectComplex(rejectingComplexId, rejectReason.trim())
      toast.success('Đã từ chối tổ hợp thành công!')
      setRejectingComplexId(null)
      setRejectReason('')
      setDetailComplex(null) // close details if open
      fetchComplexes(activeTab)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể từ chối tổ hợp'
      toast.error(msg)
    } finally {
      setIsSubmittingReject(false)
    }
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-5xl">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900">Duyệt Tổ hợp sân</h1>
          <p className="text-slate-500 text-sm mt-1">Xem xét hồ sơ thông tin và phê duyệt các tổ hợp sân thể thao (L1).</p>
        </div>
      </div>

      <Tabs value={activeTab.toLowerCase()} onValueChange={(val) => setActiveTab(val.toUpperCase())}>
        <TabsList className="mb-6">
          <TabsTrigger value="pending" className="px-4 py-2 font-semibold">
            Chờ duyệt
          </TabsTrigger>
          <TabsTrigger value="approved" className="px-4 py-2 font-semibold">
            Đã duyệt
          </TabsTrigger>
          <TabsTrigger value="rejected" className="px-4 py-2 font-semibold">
            Từ chối
          </TabsTrigger>
        </TabsList>

        {loading ? (
          <div className="flex justify-center items-center py-20">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <>
            <TabsContent value="pending" className="mt-0">
              {complexes.length === 0 ? (
                <div className="text-center py-20 text-muted-foreground border border-dashed rounded-2xl bg-white">
                  Hiện tại không có tổ hợp nào đang chờ duyệt.
                </div>
              ) : (
                complexes.map((complex) => (
                  <ComplexRow
                    key={complex.complexId}
                    complex={complex}
                    actionLoadingId={actionLoadingId}
                    handleApprove={handleApprove}
                    setRejectingComplexId={setRejectingComplexId}
                    onViewDetails={setDetailComplex}
                  />
                ))
              )}
            </TabsContent>

            <TabsContent value="approved" className="mt-0">
              {complexes.length === 0 ? (
                <div className="text-center py-20 text-muted-foreground border border-dashed rounded-2xl bg-white">
                  Chưa có tổ hợp nào được phê duyệt.
                </div>
              ) : (
                complexes.map((complex) => (
                  <ComplexRow
                    key={complex.complexId}
                    complex={complex}
                    actionLoadingId={actionLoadingId}
                    handleApprove={handleApprove}
                    setRejectingComplexId={setRejectingComplexId}
                    onViewDetails={setDetailComplex}
                  />
                ))
              )}
            </TabsContent>

            <TabsContent value="rejected" className="mt-0">
              {complexes.length === 0 ? (
                <div className="text-center py-20 text-muted-foreground border border-dashed rounded-2xl bg-white">
                  Chưa có tổ hợp nào bị từ chối.
                </div>
              ) : (
                complexes.map((complex) => (
                  <ComplexRow
                    key={complex.complexId}
                    complex={complex}
                    actionLoadingId={actionLoadingId}
                    handleApprove={handleApprove}
                    setRejectingComplexId={setRejectingComplexId}
                    onViewDetails={setDetailComplex}
                  />
                ))
              )}
            </TabsContent>
          </>
        )}
      </Tabs>

      {/* Rejection Reason Modal */}
      <Dialog
        open={rejectingComplexId !== null}
        onOpenChange={(open) => {
          if (!open) {
            setRejectingComplexId(null)
            setRejectReason('')
          }
        }}
      >
        <DialogContent className="sm:max-w-[420px]">
          <DialogHeader>
            <DialogTitle className="text-lg font-bold">Từ chối phê duyệt Tổ hợp</DialogTitle>
            <DialogDescription>
              Vui lòng cung cấp lý do từ chối để thông báo lại cho Chủ sân.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleRejectSubmit} className="space-y-4 py-2">
            <div className="space-y-2">
              <Label htmlFor="reject-reason">Lý do từ chối <span className="text-red-500">*</span></Label>
              <Input
                id="reject-reason"
                placeholder="Ví dụ: Ảnh chụp mờ, sai địa chỉ, số điện thoại liên lạc không đúng..."
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                disabled={isSubmittingReject}
              />
            </div>
            <DialogFooter className="pt-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setRejectingComplexId(null)
                  setRejectReason('')
                }}
                disabled={isSubmittingReject}
              >
                Hủy
              </Button>
              <Button type="submit" variant="destructive" disabled={isSubmittingReject}>
                {isSubmittingReject ? 'Đang gửi...' : 'Từ chối'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Complex Detail dialog (Xem Chi Tiết) */}
      <Dialog
        open={detailComplex !== null}
        onOpenChange={(open) => !open && setDetailComplex(null)}
      >
        {detailComplex && (
          <DialogContent className="sm:max-w-[680px] max-h-[85vh] overflow-y-auto">
            <DialogHeader className="border-b pb-3">
              <DialogTitle className="text-xl font-extrabold text-slate-800 flex items-center gap-2">
                <Info className="w-5 h-5 text-primary" />
                Hồ sơ chi tiết Tổ hợp
              </DialogTitle>
              <DialogDescription>
                Tổ hợp: <strong className="text-slate-700">{detailComplex.name}</strong>
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-6 py-4">
              
              {/* Image Album Gallery */}
              <div>
                <Label className="text-sm font-bold text-slate-800 mb-2 block">Album hình ảnh đăng ký ({detailComplex.imageUrls?.length || 0})</Label>
                {detailComplex.imageUrls && detailComplex.imageUrls.length > 0 ? (
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                    {detailComplex.imageUrls.map((url, idx) => (
                      <div key={idx} className="relative aspect-video rounded-lg overflow-hidden border">
                        <img
                          src={url}
                          alt={`${detailComplex.name} - ${idx}`}
                          className="w-full h-full object-cover hover:scale-105 transition-transform"
                        />
                        {idx === 0 && <Badge className="absolute top-1.5 left-1.5 text-[9px] px-1 py-0 h-4">Ảnh bìa</Badge>}
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center p-8 bg-slate-50 border border-dashed rounded-lg text-slate-400">
                    <ImageOff className="h-10 w-10 mb-2" />
                    <span className="text-xs font-semibold">Không có hình ảnh nào được tải lên</span>
                  </div>
                )}
              </div>

              {/* Basic Meta Details */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-50 p-4 rounded-xl border border-slate-100 text-sm">
                <div className="space-y-2.5">
                  <div className="text-slate-500 font-semibold text-xs uppercase">Thông tin Tổ hợp</div>
                  <div className="font-bold text-slate-800 text-base">{detailComplex.name}</div>
                  <div className="flex items-start gap-1.5 text-slate-600">
                    <MapPin className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
                    <span>{detailComplex.address}</span>
                  </div>
                  {detailComplex.phone && (
                    <div className="flex items-center gap-1.5 text-slate-600">
                      <Phone className="w-4 h-4 text-slate-400 shrink-0" />
                      <span>{detailComplex.phone}</span>
                    </div>
                  )}
                </div>

                <div className="space-y-2.5">
                  <div className="text-slate-500 font-semibold text-xs uppercase">Hệ thống</div>
                  <div className="flex items-center gap-1.5 text-slate-600">
                    <Calendar className="w-4 h-4 text-slate-400 shrink-0" />
                    <span>Đăng ký lúc: {detailComplex.createdAt ? format(new Date(detailComplex.createdAt), 'dd/MM/yyyy HH:mm') : '—'}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-slate-600">Trạng thái duyệt:</span>
                    <Badge variant="outline" className="font-bold capitalize">{detailComplex.approvedStatus}</Badge>
                  </div>
                </div>
              </div>

              {/* Description */}
              {detailComplex.description && (
                <div className="space-y-1.5">
                  <Label className="text-sm font-bold text-slate-800">Mô tả giới thiệu</Label>
                  <p className="text-slate-600 text-sm bg-slate-50 p-3 rounded-lg border border-slate-100 whitespace-pre-line leading-relaxed">
                    {detailComplex.description}
                  </p>
                </div>
              )}

              {/* Sport types & Amenities grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Sport Types */}
                <div className="space-y-2">
                  <Label className="text-sm font-bold text-slate-800">Môn thể thao hỗ trợ</Label>
                  <div className="flex flex-wrap gap-1.5">
                    {detailComplex.sportNames && detailComplex.sportNames.length > 0 ? (
                      detailComplex.sportNames.map((name) => (
                        <Badge key={name} className="bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border-0 text-xs font-semibold py-1">
                          {name}
                        </Badge>
                      ))
                    ) : (
                      <span className="text-xs text-muted-foreground font-semibold">Chưa thiết lập</span>
                    )}
                  </div>
                </div>

                {/* Amenities */}
                <div className="space-y-2">
                  <Label className="text-sm font-bold text-slate-800">Tiện ích sẵn có</Label>
                  <div className="flex flex-wrap gap-1.5">
                    {detailComplex.amenityIds && detailComplex.amenityIds.length > 0 ? (
                      detailComplex.amenityIds.map((aid) => {
                        const name = amenities.find(a => a.amenityId === aid)?.name || `Tiện ích ${aid}`
                        return (
                          <Badge key={aid} variant="outline" className="text-slate-600 text-xs font-medium py-1">
                            {name}
                          </Badge>
                        )
                      })
                    ) : (
                      <span className="text-xs text-muted-foreground font-semibold">Không có tiện ích</span>
                    )}
                  </div>
                </div>
              </div>

              {/* Rejection Reason display */}
              {detailComplex.approvedStatus === 'REJECTED' && detailComplex.rejectionReason && (
                <div className="bg-rose-50 border border-rose-100 text-rose-700 p-4 rounded-xl text-xs space-y-1">
                  <strong className="text-rose-800 font-bold block text-sm">Lý do từ chối phê duyệt:</strong>
                  <p className="leading-relaxed">{detailComplex.rejectionReason}</p>
                </div>
              )}

            </div>

            <DialogFooter className="border-t pt-4">
              <Button type="button" variant="outline" onClick={() => setDetailComplex(null)}>
                Đóng
              </Button>
              {detailComplex.approvedStatus === 'PENDING' && (
                <div className="flex gap-2">
                  <Button
                    variant="destructive"
                    onClick={() => setRejectingComplexId(detailComplex.complexId)}
                    disabled={actionLoadingId !== null}
                    className="font-bold gap-1.5"
                  >
                    <XCircle className="w-4 h-4" />
                    Từ chối
                  </Button>
                  <Button
                    onClick={() => handleApprove(detailComplex.complexId)}
                    disabled={actionLoadingId !== null}
                    className="font-bold gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white"
                  >
                    {actionLoadingId === detailComplex.complexId ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <CheckCircle className="w-4 h-4" />
                    )}
                    Phê duyệt
                  </Button>
                </div>
              )}
            </DialogFooter>
          </DialogContent>
        )}
      </Dialog>
    </div>
  )
}

const getStatusBadge = (status: string) => {
  switch (status) {
    case 'AVAILABLE':
      return <Badge className="bg-green-100 text-green-700 hover:bg-green-100 border-green-200">Hoạt động</Badge>
    case 'MAINTENANCE':
      return <Badge className="bg-yellow-100 text-yellow-700 hover:bg-yellow-100 border-yellow-200">Bảo trì</Badge>
    case 'CLOSED':
      return <Badge className="bg-red-100 text-red-700 hover:bg-red-100 border-red-200">Đóng cửa</Badge>
    default:
      return <Badge variant="outline">{status}</Badge>
  }
}

interface ComplexRowProps {
  complex: ComplexResponse
  actionLoadingId: number | null
  handleApprove: (complexId: number) => void
  setRejectingComplexId: (complexId: number) => void
  onViewDetails: (complex: ComplexResponse) => void
}

const ComplexRow = ({
  complex,
  actionLoadingId,
  handleApprove,
  setRejectingComplexId,
  onViewDetails
}: ComplexRowProps) => (
  <Card className="mb-4 overflow-hidden border-slate-200 hover:shadow-sm transition-shadow">
    <CardContent className="p-6">
      <div className="flex flex-col md:flex-row gap-6">
        {/* Cover image preview using standard img to prevent crashes */}
        <div className="w-full md:w-56 h-36 relative bg-muted rounded-lg overflow-hidden shrink-0 border border-slate-100">
          {complex.imageUrls && complex.imageUrls.length > 0 ? (
            <img
              src={complex.imageUrls[0]}
              alt={complex.name}
              className="w-full h-full object-cover"
            />
          ) : complex.coverImageUrl ? (
            <img
              src={complex.coverImageUrl}
              alt={complex.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-muted-foreground">
              <ImageOff className="h-8 w-8" />
            </div>
          )}
        </div>

        {/* Details */}
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-2 mb-2">
            <h3 className="text-xl font-bold text-slate-900 truncate">{complex.name}</h3>
            {getStatusBadge(complex.complexStatus)}
          </div>

          <div className="space-y-1.5 text-sm text-muted-foreground mb-4">
            <p className="flex items-start gap-1">
              <MapPin className="h-4 w-4 mt-0.5 shrink-0 text-slate-400" />
              <span className="text-slate-700">{complex.address}</span>
            </p>
            {complex.phone && (
              <p className="flex items-center gap-1">
                <Phone className="h-4 w-4 shrink-0 text-slate-400" />
                <span className="text-slate-700">{complex.phone}</span>
              </p>
            )}
            {complex.createdAt && (
              <p className="flex items-center gap-1">
                <Calendar className="h-4 w-4 shrink-0 text-slate-400" />
                <span className="text-slate-500">
                  Ngày đăng ký: {format(new Date(complex.createdAt), 'dd/MM/yyyy HH:mm')}
                </span>
              </p>
            )}

            {/* Sport Type Badge list */}
            {complex.sportNames && complex.sportNames.length > 0 && (
              <div className="flex flex-wrap items-center gap-1.5 pt-1.5">
                <span className="text-xs font-bold text-slate-500 mr-1">Môn thể thao:</span>
                {complex.sportNames.map((name) => (
                  <Badge key={name} variant="secondary" className="text-[10px] font-bold tracking-wide uppercase bg-emerald-50 text-emerald-700 border-0">
                    {name}
                  </Badge>
                ))}
              </div>
            )}

            {/* Display Rejection Reason if in Rejected Tab */}
            {complex.approvedStatus === 'REJECTED' && complex.rejectionReason && (
              <div className="bg-rose-50 border border-rose-100 rounded-lg p-3 mt-3 text-xs text-rose-700">
                <strong>Lý do từ chối:</strong> {complex.rejectionReason}
              </div>
            )}
          </div>

          {/* Action buttons */}
          <div className="flex flex-wrap items-center gap-2">
            <Button
              size="sm"
              variant="outline"
              onClick={() => onViewDetails(complex)}
              className="font-semibold gap-1.5 text-xs text-slate-700"
            >
              <Eye className="w-3.5 h-3.5" />
              Xem Chi Tiết
            </Button>

            {complex.approvedStatus === 'PENDING' && (
              <div className="flex gap-2">
                <Button
                  size="sm"
                  className="font-semibold text-xs"
                  disabled={actionLoadingId !== null}
                  onClick={() => handleApprove(complex.complexId)}
                >
                  {actionLoadingId === complex.complexId ? (
                    <Loader2 className="h-3.5 w-3.5 animate-spin mr-1.5" />
                  ) : (
                    <CheckCircle className="h-3.5 w-3.5 mr-1.5" />
                  )}
                  Phê duyệt
                </Button>
                <Button
                  size="sm"
                  variant="destructive"
                  className="font-semibold text-xs"
                  disabled={actionLoadingId !== null}
                  onClick={() => setRejectingComplexId(complex.complexId)}
                >
                  <XCircle className="h-3.5 w-3.5 mr-1.5" />
                  Từ chối
                </Button>
              </div>
            )}
          </div>
        </div>
      </div>
    </CardContent>
  </Card>
)
