"use client"

import * as React from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Badge } from "@/components/ui/badge"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { toast } from "sonner"
import { Plus, Loader2, Trash2, Zap, Clock, CalendarDays } from "lucide-react"
import {
  getStadiumTimeSlots,
  createTimeSlot,
  bulkCreateTimeSlots,
  deleteTimeSlot,
  toggleTimeSlot,
} from "@/lib/api/timeSlot"
import { TimeSlot, CreateTimeSlotRequest } from "@/types/timeSlot"
import { ConfirmDialog } from "@/components/common/ConfirmDialog"
import { useConfirm } from "@/hooks/useConfirm"

interface TimeSlotManagerProps {
  stadiumId: number
  openTime: string // HH:mm:ss
  closeTime: string // HH:mm:ss
}

export function TimeSlotManager({
  stadiumId,
  openTime,
  closeTime,
}: TimeSlotManagerProps) {
  const [slots, setSlots] = React.useState<TimeSlot[]>([])
  const [loading, setLoading] = React.useState<boolean>(true)
  const [submitting, setSubmitting] = React.useState<boolean>(false)

  // Custom confirm hook
  const { 
    isOpen: isConfirmOpen, 
    isLoading: isDeleting, 
    options: confirmOptions, 
    confirm, 
    close: closeConfirm, 
    execute: executeDelete 
  } = useConfirm()

  // Form states
  const [startTime, setStartTime] = React.useState<string>("")
  const [endTime, setEndTime] = React.useState<string>("")
  const [pricePerSlot, setPricePerSlot] = React.useState<string>("")
  
  // Bulk states
  const [bulkPrice, setBulkPrice] = React.useState<string>("")

  const fetchSlots = React.useCallback(async () => {
    try {
      setLoading(true)
      const data = await getStadiumTimeSlots(stadiumId)
      setSlots(data.sort((a, b) => a.startTime.localeCompare(b.startTime)))
    } catch (error: any) {
      toast.error("Không thể tải danh sách khung giờ")
    } finally {
      setLoading(false)
    }
  }, [stadiumId])

  React.useEffect(() => {
    fetchSlots()
  }, [fetchSlots])

  const checkOverlap = (start: string, end: string) => {
    // Ensure format is HH:mm:ss for comparison
    const s = start.length === 5 ? `${start}:00` : start
    const e = end.length === 5 ? `${end}:00` : end
    return slots.some((slot) => {
      return s < slot.endTime && e > slot.startTime
    })
  }

  const handleAddSlot = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!startTime || !endTime || !pricePerSlot) {
      toast.error("Vui lòng điền đầy đủ thông tin")
      return
    }

    if (startTime >= endTime) {
      toast.error("Giờ bắt đầu phải trước giờ kết thúc")
      return
    }

    // Client-side operating hours check
    if (startTime < openTime.substring(0, 5) || endTime > closeTime.substring(0, 5)) {
      toast.error(`Khung giờ phải nằm trong khoảng ${openTime.substring(0, 5)} - ${closeTime.substring(0, 5)}`)
      return
    }

    if (checkOverlap(startTime, endTime)) {
      toast.error("Khung giờ bị trùng lặp với khung giờ đã có")
      return
    }

    try {
      setSubmitting(true)
      await createTimeSlot(stadiumId, {
        startTime: `${startTime}:00`,
        endTime: `${endTime}:00`,
        pricePerSlot: parseFloat(pricePerSlot),
      })
      toast.success("Đã thêm khung giờ mới")
      setStartTime("")
      setEndTime("")
      setPricePerSlot("")
      fetchSlots()
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Lỗi khi thêm khung giờ")
    } finally {
      setSubmitting(false)
    }
  }

  const handleDeleteSlot = (slotId: number) => {
    confirm({
      title: "Xác nhận xóa khung giờ",
      description: "Bạn có chắc chắn muốn xóa khung giờ này không? Khách hàng sẽ không thể đặt sân vào khoảng thời gian này.",
      confirmText: "Xóa ngay",
      variant: "destructive",
      onConfirm: async () => {
        await deleteTimeSlot(slotId)
        setSlots(prev => prev.filter(s => s.slotId !== slotId))
        toast.success("Đã xóa khung giờ thành công")
      }
    })
  }

  const handleToggle = async (slotId: number) => {
    try {
      const updated = await toggleTimeSlot(slotId)
      setSlots(slots.map((s) => (s.slotId === slotId ? updated : s)))
      toast.success("Đã cập nhật trạng thái")
    } catch (error: any) {
      toast.error("Không thể cập nhật trạng thái")
    }
  }

  const handleBulkGenerate = async () => {
    if (!bulkPrice) {
      toast.error("Vui lòng nhập giá cơ bản cho các khung giờ")
      return
    }

    const price = parseFloat(bulkPrice)
    const newSlots: CreateTimeSlotRequest[] = []
    
    let currentHour = parseInt(openTime.split(":")[0])
    const endHour = parseInt(closeTime.split(":")[0])

    while (currentHour < endHour) {
      const start = `${currentHour.toString().padStart(2, "0")}:00:00`
      const end = `${(currentHour + 1).toString().padStart(2, "0")}:00:00`
      
      if (!checkOverlap(start, end)) {
        newSlots.push({
          startTime: start,
          endTime: end,
          pricePerSlot: price,
        })
      }
      currentHour++
    }

    if (newSlots.length === 0) {
      toast.info("Không có khung giờ mới nào được tạo (đã tồn tại hoặc ngoài giờ mở cửa)")
      return
    }

    try {
      setSubmitting(true)
      await bulkCreateTimeSlots(stadiumId, newSlots)
      toast.success(`Đã tự động tạo ${newSlots.length} khung giờ`)
      setBulkPrice("")
      fetchSlots()
    } catch (error: any) {
      toast.error("Lỗi khi tạo khung giờ hàng loạt")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="space-y-8 bg-white p-6 rounded-lg border shadow-sm">
      <div className="flex items-center justify-between border-b pb-4">
        <div>
          <h2 className="text-xl font-bold flex items-center gap-2 text-slate-800">
            <Clock className="w-5 h-5 text-primary" />
            Quản lý khung giờ
          </h2>
          <p className="text-sm text-slate-500 mt-1 font-medium">
            Giờ hoạt động: <span className="text-slate-700">{openTime.substring(0, 5)} - {closeTime.substring(0, 5)}</span>
          </p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <Input
              type="number"
              placeholder="Giá mặc định..."
              value={bulkPrice}
              onChange={(e) => setBulkPrice(e.target.value)}
              className="w-32 h-9 border-slate-200"
            />
            <Button
              variant="outline"
              size="sm"
              onClick={handleBulkGenerate}
              disabled={submitting || loading}
              className="gap-2 border-primary/30 text-primary hover:bg-primary/5 font-semibold"
            >
              {submitting ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <Zap className="w-4 h-4 text-yellow-500 fill-yellow-500" />
              )}
              Tự động tạo
            </Button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <h3 className="text-lg font-semibold mb-4 flex items-center gap-2 text-slate-700">
            <CalendarDays className="w-4 h-4 text-slate-400" />
            Danh sách khung giờ hiện tại
          </h3>
          
          {loading ? (
            <div className="flex flex-col items-center justify-center py-20 text-slate-400 bg-slate-50/50 rounded-lg border border-dashed">
              <Loader2 className="w-10 h-10 animate-spin text-primary/40 mb-3" />
              <p className="text-sm font-medium tracking-wide">Đang đồng bộ dữ liệu...</p>
            </div>
          ) : slots.length === 0 ? (
            <div className="text-center py-20 border-2 border-dashed rounded-lg bg-slate-50/30">
              <Clock className="w-12 h-12 text-slate-200 mx-auto mb-3" />
              <p className="text-slate-400 font-medium">Chưa có khung giờ nào được thiết lập</p>
            </div>
          ) : (
            <div className="border border-slate-200 rounded-lg overflow-hidden shadow-xs">
              <Table>
                <TableHeader className="bg-slate-50/50">
                  <TableRow>
                    <TableHead className="font-bold text-slate-600">Thời gian</TableHead>
                    <TableHead className="font-bold text-slate-600">Giá thuê (đ)</TableHead>
                    <TableHead className="font-bold text-slate-600">Trạng thái</TableHead>
                    <TableHead className="text-right font-bold text-slate-600 pr-6">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {slots.map((slot) => (
                    <TableRow key={slot.slotId} className="hover:bg-slate-50/50 transition-colors">
                      <TableCell className="font-semibold text-slate-700 py-4">
                        {slot.startTime.substring(0, 5)} - {slot.endTime.substring(0, 5)}
                      </TableCell>
                      <TableCell className="text-primary font-bold">
                        {slot.pricePerSlot.toLocaleString('vi-VN')}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={slot.slotStatus === "AVAILABLE" ? "default" : "secondary"}
                          className={slot.slotStatus === "AVAILABLE" ? "bg-emerald-100 text-emerald-700 hover:bg-emerald-100 border-none px-3" : "px-3"}
                        >
                          {slot.slotStatus === "AVAILABLE" ? "Đang mở" : 
                           slot.slotStatus === "BOOKED" ? "Đã đặt" : "Bảo trì"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right space-x-1 pr-4">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleToggle(slot.slotId)}
                          disabled={slot.slotStatus === "BOOKED"}
                          className="hover:bg-slate-100 rounded-full"
                          title={slot.slotStatus === "AVAILABLE" ? "Tạm đóng" : "Mở lại"}
                        >
                          <Switch
                            checked={slot.slotStatus === "AVAILABLE"}
                            disabled={slot.slotStatus === "BOOKED"}
                          />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-full transition-all"
                          onClick={() => handleDeleteSlot(slot.slotId)}
                          disabled={slot.slotStatus === "BOOKED"}
                        >
                          <Trash2 className="w-4.5 h-4.5" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </div>

        <div className="space-y-4">
          <div className="p-5 border border-slate-200 rounded-xl bg-slate-50/50 shadow-sm">
            <h3 className="text-lg font-bold mb-5 flex items-center gap-2 text-slate-800">
              <div className="p-1.5 bg-primary/10 rounded-lg">
                <Plus className="w-4 h-4 text-primary" />
              </div>
              Thêm khung giờ
            </h3>
            <form onSubmit={handleAddSlot} className="space-y-5">
              <div className="space-y-2">
                <Label htmlFor="startTime" className="text-xs font-bold uppercase tracking-wider text-slate-500">Giờ bắt đầu</Label>
                <Input
                  id="startTime"
                  type="time"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className="bg-white border-slate-200 focus:ring-primary/20"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="endTime" className="text-xs font-bold uppercase tracking-wider text-slate-500">Giờ kết thúc</Label>
                <Input
                  id="endTime"
                  type="time"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  className="bg-white border-slate-200 focus:ring-primary/20"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="price" className="text-xs font-bold uppercase tracking-wider text-slate-500">Giá thuê (đ)</Label>
                <Input
                  id="price"
                  type="number"
                  placeholder="150,000"
                  value={pricePerSlot}
                  onChange={(e) => setPricePerSlot(e.target.value)}
                  className="bg-white border-slate-200 focus:ring-primary/20"
                />
              </div>
              <Button type="submit" className="w-full gap-2 h-11 font-bold shadow-md shadow-primary/20" disabled={submitting}>
                {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
                Lưu khung giờ
              </Button>
            </form>
          </div>
          
          <div className="p-4 border border-blue-100 rounded-xl bg-blue-50/50 text-blue-800 text-sm leading-relaxed">
            <div className="flex items-center gap-2 mb-2">
              <Zap className="w-4 h-4 text-blue-600 fill-blue-600" />
              <span className="font-bold">Mẹo tối ưu</span>
            </div>
            Sử dụng <span className="font-bold">Tự động tạo</span> để hệ thống tự tính toán các khung giờ 1 tiếng phù hợp với giờ mở cửa của bạn.
          </div>
        </div>
      </div>

      <ConfirmDialog
        isOpen={isConfirmOpen}
        onClose={closeConfirm}
        onConfirm={executeDelete}
        isLoading={isDeleting}
        title={confirmOptions?.title || ""}
        description={confirmOptions?.description || ""}
        confirmText={confirmOptions?.confirmText}
        variant={confirmOptions?.variant}
      />
    </div>
  )
}
