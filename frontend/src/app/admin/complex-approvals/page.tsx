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
import { CheckCircle, XCircle, MapPin, Phone, Loader2, ImageOff, Calendar } from 'lucide-react'
import { stadiumService } from '@/lib/services/stadium'
import { ComplexResponse } from '@/types/stadium'
import { toast } from 'sonner'
import Image from 'next/image'
import { format } from 'date-fns'

export default function ComplexApprovalPage() {
  const [complexes, setComplexes] = useState<ComplexResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null)
  const [activeTab, setActiveTab] = useState('PENDING')

  // Inline Rejection state
  const [rejectingComplexId, setRejectingComplexId] = useState<number | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const [isSubmittingReject, setIsSubmittingReject] = useState(false)

  const fetchComplexes = (status: string) => {
    setLoading(true)
    stadiumService.getAllComplexesAdmin(status)
      .then(setComplexes)
      .catch(() => toast.error('Không thể tải danh sách tổ hợp chờ duyệt'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    fetchComplexes(activeTab)
  }, [activeTab])

  const handleApprove = async (complexId: number) => {
    setActionLoadingId(complexId)
    try {
      await stadiumService.approveComplex(complexId)
      toast.success('Đã duyệt tổ hợp thành công!')
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
          <p className="text-slate-500 text-sm mt-1">Review and approve new sport complexes or changes.</p>
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
}

const ComplexRow = ({
  complex,
  actionLoadingId,
  handleApprove,
  setRejectingComplexId
}: ComplexRowProps) => (
  <Card className="mb-4 overflow-hidden border-slate-200 hover:shadow-sm transition-shadow">
    <CardContent className="p-6">
      <div className="flex flex-col md:flex-row gap-6">
        {/* Cover image preview */}
        <div className="w-full md:w-56 h-36 relative bg-muted rounded-lg overflow-hidden shrink-0 border border-slate-100">
          {complex.imageUrls && complex.imageUrls.length > 0 ? (
            <Image
              src={complex.imageUrls[0]}
              alt={complex.name}
              fill
              className="object-cover"
              unoptimized
            />
          ) : complex.coverImageUrl ? (
            <Image
              src={complex.coverImageUrl}
              alt={complex.name}
              fill
              className="object-cover"
              unoptimized
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
          {complex.approvedStatus === 'PENDING' && (
            <div className="flex gap-3 max-w-xs">
              <Button
                size="sm"
                className="flex-1 font-semibold"
                disabled={actionLoadingId !== null}
                onClick={() => handleApprove(complex.complexId)}
              >
                {actionLoadingId === complex.complexId ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-1.5" />
                ) : (
                  <CheckCircle className="h-4 w-4 mr-1.5" />
                )}
                Phê duyệt
              </Button>
              <Button
                size="sm"
                variant="destructive"
                className="flex-1 font-semibold"
                disabled={actionLoadingId !== null}
                onClick={() => setRejectingComplexId(complex.complexId)}
              >
                <XCircle className="h-4 w-4 mr-1.5" />
                Từ chối
              </Button>
            </div>
          )}
        </div>
      </div>
    </CardContent>
  </Card>
)
