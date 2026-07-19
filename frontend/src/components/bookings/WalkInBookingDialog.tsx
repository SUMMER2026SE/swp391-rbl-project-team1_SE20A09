import * as React from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { createWalkInBooking } from "@/lib/bookings-api"
import { toast } from "sonner"
import { Loader2, UserPlus, MapPin, Calendar, Clock, DollarSign } from "lucide-react"

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
}: WalkInBookingDialogProps) {
  const [submitting, setSubmitting] = React.useState(false)

  const handleConfirm = async () => {
    try {
      setSubmitting(true)
      await createWalkInBooking({
        stadiumId,
        slotId,
        reservationDate: date
      })
      toast.success("Tạo đơn vãng lai thành công. Đã thu tiền mặt.")
      onSuccess()
      onClose()
    } catch (error: any) {
      toast.error(error.message || "Lỗi khi tạo đơn vãng lai")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[420px] bg-white">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-xl font-bold text-slate-800">
            <div className="p-2 bg-blue-100 rounded-full text-blue-600">
              <UserPlus className="w-5 h-5" />
            </div>
            Tạo Đơn Khách Vãng Lai
          </DialogTitle>
          <DialogDescription>
            Đơn này sẽ được xác nhận ngay lập tức và ghi nhận đã thu tiền mặt tại sân. Không yêu cầu thông tin khách hàng.
          </DialogDescription>
        </DialogHeader>

        <div className="py-4 space-y-3">
          <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg border border-slate-100">
            <Calendar className="w-5 h-5 text-slate-400" />
            <div>
              <p className="text-xs text-slate-500 font-medium">Ngày chơi</p>
              <p className="text-sm font-semibold text-slate-800">
                {new Date(date).toLocaleDateString("vi-VN", { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-lg border border-slate-100">
            <Clock className="w-5 h-5 text-slate-400" />
            <div>
              <p className="text-xs text-slate-500 font-medium">Khung giờ</p>
              <p className="text-sm font-semibold text-slate-800">{startTime} - {endTime}</p>
            </div>
          </div>

          <div className="flex items-center gap-3 p-3 bg-emerald-50 rounded-lg border border-emerald-100">
            <DollarSign className="w-5 h-5 text-emerald-500" />
            <div>
              <p className="text-xs text-emerald-600 font-medium">Tổng thu (Tiền mặt)</p>
              <p className="text-lg font-bold text-emerald-700">{price.toLocaleString('vi-VN')}đ</p>
            </div>
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0 pt-2">
          <Button type="button" variant="outline" onClick={onClose} disabled={submitting}>
            Hủy
          </Button>
          <Button type="button" onClick={handleConfirm} disabled={submitting} className="bg-blue-600 hover:bg-blue-700 text-white gap-2">
            {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
            Xác nhận đã thu tiền
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
