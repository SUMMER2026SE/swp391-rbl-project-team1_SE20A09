import * as React from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { Badge } from "@/components/ui/badge"
import { createWalkInBooking } from "@/lib/bookings-api"
import { getVenueDetail } from "@/lib/api/venue"
import { toast } from "sonner"
import {
  Loader2, UserPlus, Calendar, Clock, CheckCircle2, ShoppingBag,
  Plus, Minus, CreditCard, Info
} from "lucide-react"

interface WalkInBookingDialogProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
  stadiumId: number
  slotId: number
  date: string
  startTime: string
  endTime: string
  price: number
  stadiumName?: string
}

type AccessoryEntry = {
  accessoryId: number
  name: string
  pricePerUnit: number
  quantity: number   // max stock
}

export function WalkInBookingDialog({
  isOpen,
  onClose,
  onSuccess,
  stadiumId,
  slotId,
  date,
  startTime,
  endTime,
  price,
  stadiumName,
}: WalkInBookingDialogProps) {
  const [submitting, setSubmitting] = React.useState(false)
  const [accessories, setAccessories] = React.useState<AccessoryEntry[]>([])
  const [loadingAccessories, setLoadingAccessories] = React.useState(true)
  const [selectedAccessories, setSelectedAccessories] = React.useState<Record<number, number>>({})

  React.useEffect(() => {
    if (isOpen) {
      setLoadingAccessories(true)
      getVenueDetail(stadiumId)
        .then(data => setAccessories((data.accessories || []) as AccessoryEntry[]))
        .catch(() => toast.error("Không thể tải danh sách phụ kiện"))
        .finally(() => setLoadingAccessories(false))
      setSelectedAccessories({})
    }
  }, [isOpen, stadiumId])

  const handleUpdateAccessory = (accId: number, delta: number, maxQty: number) => {
    setSelectedAccessories(prev => {
      const current = prev[accId] || 0
      const next = Math.max(0, Math.min(current + delta, maxQty))
      const res = { ...prev }
      if (next === 0) delete res[accId]
      else res[accId] = next
      return res
    })
  }

  const accessoryLines = accessories
    .filter(acc => (selectedAccessories[acc.accessoryId] || 0) > 0)
    .map(acc => ({
      ...acc,
      qty: selectedAccessories[acc.accessoryId]!,
      lineTotal: acc.pricePerUnit * selectedAccessories[acc.accessoryId]!,
    }))

  const accessoryTotal = accessoryLines.reduce((sum, l) => sum + l.lineTotal, 0)
  const totalPrice = price + accessoryTotal

  const playDateFormatted = (() => {
    try {
      const [y, m, d] = date.split("-").map(Number)
      return new Date(y, m - 1, d).toLocaleDateString("vi-VN", {
        weekday: "long", day: "numeric", month: "long", year: "numeric",
      })
    } catch { return date }
  })()

  const handleConfirm = async () => {
    try {
      setSubmitting(true)
      const accList = Object.entries(selectedAccessories).map(([id, qty]) => ({
        accessoryId: Number(id),
        quantity: qty,
      }))
      await createWalkInBooking({
        stadiumId,
        slotId,
        reservationDate: date,
        accessories: accList.length > 0 ? accList : undefined,
      })
      toast.success("Đã tạo đơn và ghi nhận thu tiền mặt thành công!")
      onSuccess()
      onClose()
    } catch (error: any) {
      toast.error(error.message || "Lỗi khi tạo đơn vãng lai")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={open => !open && onClose()}>
      <DialogContent className="sm:max-w-[560px] bg-white p-0 overflow-hidden rounded-2xl">

        {/* ── Header ── */}
        <DialogHeader className="px-6 pt-6 pb-4 border-b border-slate-100">
          <DialogTitle className="flex items-center gap-3 text-lg font-bold text-slate-800">
            <div className="p-2 bg-blue-100 rounded-xl">
              <UserPlus className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <p className="leading-tight">Tạo Đơn Khách Vãng Lai</p>
              {stadiumName && (
                <p className="text-sm font-medium text-slate-500 mt-0.5">{stadiumName}</p>
              )}
            </div>
            <Badge variant="outline" className="ml-auto text-xs text-blue-600 border-blue-200 bg-blue-50 font-semibold">
              Tiền mặt
            </Badge>
          </DialogTitle>
        </DialogHeader>

        <div className="px-6 py-4 space-y-4 max-h-[70vh] overflow-y-auto">

          {/* ── Booking Info Card ── */}
          <div className="grid grid-cols-2 gap-3">
            <div className="flex items-start gap-2.5 p-3.5 bg-slate-50 rounded-xl border border-slate-100">
              <Calendar className="w-4 h-4 text-slate-400 mt-0.5 shrink-0" />
              <div>
                <p className="text-[11px] text-slate-400 font-semibold uppercase tracking-wide mb-0.5">Ngày chơi</p>
                <p className="text-sm font-bold text-slate-800 leading-tight">{playDateFormatted}</p>
              </div>
            </div>
            <div className="flex items-start gap-2.5 p-3.5 bg-slate-50 rounded-xl border border-slate-100">
              <Clock className="w-4 h-4 text-slate-400 mt-0.5 shrink-0" />
              <div>
                <p className="text-[11px] text-slate-400 font-semibold uppercase tracking-wide mb-0.5">Khung giờ</p>
                <p className="text-sm font-bold text-slate-800">{startTime} – {endTime}</p>
              </div>
            </div>
          </div>

          {/* ── Notice ── */}
          <div className="flex items-start gap-2 p-3 bg-amber-50 border border-amber-100 rounded-xl text-xs text-amber-700">
            <Info className="w-3.5 h-3.5 mt-0.5 shrink-0" />
            <span>Đơn vãng lai sẽ xác nhận ngay và ghi nhận đã thu tiền mặt tại sân. Không yêu cầu thông tin khách hàng.</span>
          </div>

          {/* ── Accessories Section ── */}
          <div>
            <div className="flex items-center gap-2 mb-3">
              <ShoppingBag className="w-4 h-4 text-slate-500" />
              <p className="text-sm font-bold text-slate-700">Phụ kiện / Dịch vụ kèm theo</p>
              <span className="ml-auto text-xs text-slate-400 font-medium">Tùy chọn</span>
            </div>

            {loadingAccessories ? (
              <div className="flex items-center justify-center py-6">
                <Loader2 className="w-5 h-5 animate-spin text-slate-400 mr-2" />
                <span className="text-sm text-slate-400">Đang tải...</span>
              </div>
            ) : accessories.length === 0 ? (
              <div className="text-center py-5 border border-dashed border-slate-200 rounded-xl bg-slate-50">
                <ShoppingBag className="w-7 h-7 text-slate-200 mx-auto mb-1.5" />
                <p className="text-xs text-slate-400 font-medium">Sân không có phụ kiện kèm theo</p>
              </div>
            ) : (
              <div className="space-y-2">
                {accessories.map(acc => {
                  const qty = selectedAccessories[acc.accessoryId] || 0
                  const selected = qty > 0
                  return (
                    <div
                      key={acc.accessoryId}
                      className={`flex items-center justify-between px-3.5 py-3 rounded-xl border transition-all ${
                        selected
                          ? "border-blue-200 bg-blue-50/60"
                          : "border-slate-200 bg-white hover:border-slate-300"
                      }`}
                    >
                      <div className="flex-1 min-w-0">
                        <p className={`text-sm font-semibold truncate ${selected ? "text-blue-800" : "text-slate-800"}`}>
                          {acc.name}
                        </p>
                        <p className={`text-xs mt-0.5 ${selected ? "text-blue-500" : "text-slate-400"}`}>
                          {acc.pricePerUnit.toLocaleString("vi-VN")}đ / đơn vị
                        </p>
                      </div>
                      <div className="flex items-center gap-2 ml-3">
                        <Button
                          type="button" variant="outline" size="icon"
                          className={`h-7 w-7 rounded-full border transition-all ${
                            qty <= 0
                              ? "opacity-30 cursor-not-allowed"
                              : "border-slate-300 hover:border-red-300 hover:bg-red-50"
                          }`}
                          onClick={() => handleUpdateAccessory(acc.accessoryId, -1, acc.quantity)}
                          disabled={qty <= 0}
                        >
                          <Minus className="h-3 w-3" />
                        </Button>
                        <span className={`w-6 text-center text-sm font-bold ${selected ? "text-blue-700" : "text-slate-500"}`}>
                          {qty}
                        </span>
                        <Button
                          type="button" variant="outline" size="icon"
                          className={`h-7 w-7 rounded-full border transition-all ${
                            qty >= acc.quantity
                              ? "opacity-30 cursor-not-allowed"
                              : "border-slate-300 hover:border-blue-300 hover:bg-blue-50"
                          }`}
                          onClick={() => handleUpdateAccessory(acc.accessoryId, 1, acc.quantity)}
                          disabled={qty >= acc.quantity}
                        >
                          <Plus className="h-3 w-3" />
                        </Button>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>

          {/* ── Price Breakdown ── */}
          <div className="rounded-xl border border-slate-200 overflow-hidden">
            <div className="px-4 py-2.5 bg-slate-50 border-b border-slate-200">
              <p className="text-xs font-bold text-slate-600 uppercase tracking-wide">Chi tiết thanh toán</p>
            </div>
            <div className="px-4 py-3 space-y-2.5">
              <div className="flex justify-between items-center text-sm">
                <span className="text-slate-600">Tiền sân ({startTime}–{endTime})</span>
                <span className="font-semibold text-slate-800">{price.toLocaleString("vi-VN")}đ</span>
              </div>

              {accessoryLines.map(l => (
                <div key={l.accessoryId} className="flex justify-between items-center text-sm">
                  <span className="text-slate-500">{l.name} × {l.qty}</span>
                  <span className="font-medium text-slate-700">+{l.lineTotal.toLocaleString("vi-VN")}đ</span>
                </div>
              ))}

              <Separator className="my-1" />

              <div className="flex justify-between items-center">
                <span className="text-sm font-bold text-slate-800">Tổng thu tiền mặt</span>
                <span className="text-lg font-extrabold text-emerald-600">{totalPrice.toLocaleString("vi-VN")}đ</span>
              </div>
            </div>
          </div>
        </div>

        {/* ── Footer ── */}
        <DialogFooter className="px-6 py-4 border-t border-slate-100 gap-2 bg-slate-50/50">
          <Button type="button" variant="outline" onClick={onClose} disabled={submitting}
            className="border-slate-200">
            Hủy bỏ
          </Button>
          <Button
            type="button"
            onClick={handleConfirm}
            disabled={submitting}
            className="flex-1 bg-emerald-600 hover:bg-emerald-700 text-white font-bold gap-2 h-10 rounded-xl"
          >
            {submitting ? (
              <><Loader2 className="w-4 h-4 animate-spin" /> Đang xử lý...</>
            ) : (
              <><CheckCircle2 className="w-4 h-4" /> Hoàn tất thanh toán ({totalPrice.toLocaleString("vi-VN")}đ)</>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
