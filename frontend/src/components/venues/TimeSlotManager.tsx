'use client'

import * as React from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { toast } from "sonner"
import { Plus, Loader2, Trash2, Zap, Clock, CalendarDays, Edit3, ChevronLeft, ChevronRight, Grid, List, Power } from "lucide-react"
import {
  getStadiumTimeSlots,
  createTimeSlot,
  bulkCreateTimeSlots,
  deleteTimeSlot,
  toggleTimeSlot,
  createOrUpdateException,
  deleteException,
} from "@/lib/api/timeSlot"
import { getWeeklySlots, WeeklySlotsResponse, WeeklySlotItem } from "@/lib/bookings-api"
import { TimeSlot, CreateTimeSlotRequest } from "@/types/timeSlot"
import { ConfirmDialog } from "@/components/common/ConfirmDialog"
import { useConfirm } from "@/hooks/useConfirm"
import { EditTimeSlotDialog } from "./EditTimeSlotDialog"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog"
import WeeklyAgendaGrid from "./WeeklyAgendaGrid"

// ── Date management helpers ──────────────────────────────────────────────────
function mondayOfThisWeek(): string {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  const dow = d.getDay()
  const diffToMonday = dow === 0 ? -6 : 1 - dow
  d.setDate(d.getDate() + diffToMonday)
  return formatYMD(d)
}

function addDays(ymd: string, n: number): string {
  const d = parseYMD(ymd)
  d.setDate(d.getDate() + n)
  return formatYMD(d)
}

function parseYMD(ymd: string): Date {
  const [y, m, d] = ymd.split('-').map(Number)
  return new Date(y, m - 1, d)
}

function formatYMD(d: Date): string {
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

function formatDayShort(ymd: string): string {
  try {
    const d = parseYMD(ymd)
    return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`
  } catch {
    return ymd
  }
}

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
  const [viewMode, setViewMode] = React.useState<"weekly" | "list">("weekly")
  const [currentDate, setCurrentDate] = React.useState<string>(() => {
    return mondayOfThisWeek()
  })

  const [weeklyData, setWeeklyData] = React.useState<WeeklySlotsResponse | null>(null)
  const [slots, setSlots] = React.useState<TimeSlot[]>([])
  const [loading, setLoading] = React.useState<boolean>(true)
  const [submitting, setSubmitting] = React.useState<boolean>(false)

  // Edit dialog state
  const [editingSlot, setEditingSlot] = React.useState<TimeSlot | null>(null)
  const [isEditOpen, setIsEditOpen] = React.useState<boolean>(false)
  const [isAddOpen, setIsAddOpen] = React.useState<boolean>(false)
  
  const [selectedSlotForChoice, setSelectedSlotForChoice] = React.useState<{
    slot: WeeklySlotItem
    date: string
    action: "edit" | "toggle" | "delete"
  } | null>(null)
  const [isChoiceOpen, setIsChoiceOpen] = React.useState<boolean>(false)
  const [selectedDate, setSelectedDate] = React.useState<string | undefined>(undefined)

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

  const loadData = React.useCallback(async () => {
    try {
      setLoading(true)
      if (viewMode === "weekly") {
        const data = await getWeeklySlots(stadiumId, currentDate)
        setWeeklyData(data)
      } else {
        const data = await getStadiumTimeSlots(stadiumId)
        setSlots(data.sort((a, b) => a.startTime.localeCompare(b.startTime)))
      }
    } catch (error: any) {
      toast.error("Không thể tải danh sách khung giờ")
    } finally {
      setLoading(false)
    }
  }, [stadiumId, currentDate, viewMode])

  React.useEffect(() => {
    loadData()
  }, [loadData])

  const navigateWeek = (direction: "prev" | "next") => {
    const offset = direction === "prev" ? -7 : 7
    setCurrentDate((prev) => addDays(prev, offset))
  }

  const checkOverlap = (start: string, end: string) => {
    const s = start.length === 5 ? `${start}:00` : start
    const e = end.length === 5 ? `${end}:00` : end
    
    if (viewMode === "weekly" && weeklyData) {
      // Check across all days' slots
      return weeklyData.days.some((day) => 
        day.slots.some((slot) => {
          const slotStart = slot.startTime.length === 5 ? `${slot.startTime}:00` : slot.startTime
          const slotEnd = slot.endTime.length === 5 ? `${slot.endTime}:00` : slot.endTime
          return s < slotEnd && e > slotStart
        })
      )
    }

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
      setIsAddOpen(false)
      loadData()
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Lỗi khi thêm khung giờ")
    } finally {
      setSubmitting(false)
    }
  }

  const handleChoiceTodayOnly = async () => {
    if (!selectedSlotForChoice) return
    const { slot, date, action } = selectedSlotForChoice
    
    setIsChoiceOpen(false)
    
    if (action === "edit") {
      setSelectedDate(date)
      setEditingSlot(toTimeSlot(slot))
      setIsEditOpen(true)
    } else if (action === "toggle") {
      try {
        if (slot.status === "OWNER_CLOSED") {
          try {
            await deleteException(slot.slotId, date)
          } catch {
            await createOrUpdateException(slot.slotId, date, { closed: false })
          }
          toast.success(`Đã mở lại khung giờ ngày ${formatDayShort(date)}`)
        } else {
          await createOrUpdateException(slot.slotId, date, { closed: true })
          toast.success(`Đã tạm đóng khung giờ ngày ${formatDayShort(date)}`)
        }
        loadData()
      } catch (error) {
        toast.error("Không thể cập nhật trạng thái")
      }
    } else if (action === "delete") {
      try {
        await createOrUpdateException(slot.slotId, date, { hidden: true })
        toast.success(`Đã xóa khung giờ ngày ${formatDayShort(date)}`)
        loadData()
      } catch (error) {
        toast.error("Không thể xóa khung giờ")
      }
    }
  }

  const handleChoiceAllDays = () => {
    if (!selectedSlotForChoice) return
    const { slot, action } = selectedSlotForChoice
    
    setIsChoiceOpen(false)
    setSelectedDate(undefined)

    if (action === "edit") {
      setEditingSlot(toTimeSlot(slot))
      setIsEditOpen(true)
    } else if (action === "toggle") {
      handleToggle(slot.slotId)
    } else if (action === "delete") {
      handleDeleteSlot(slot.slotId)
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
        toast.success("Đã xóa khung giờ thành công")
        loadData()
      }
    })
  }

  const handleToggle = async (slotId: number) => {
    try {
      await toggleTimeSlot(slotId)
      toast.success("Đã cập nhật trạng thái")
      loadData()
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
      loadData()
    } catch (error: any) {
      toast.error("Lỗi khi tạo khung giờ hàng loạt")
    } finally {
      setSubmitting(false)
    }
  }

  const toTimeSlot = (item: WeeklySlotItem): TimeSlot => ({
    slotId: item.slotId,
    stadiumId: stadiumId,
    startTime: item.startTime.length === 5 ? `${item.startTime}:00` : item.startTime,
    endTime: item.endTime.length === 5 ? `${item.endTime}:00` : item.endTime,
    pricePerSlot: item.price,
    slotStatus: item.status === "AVAILABLE" ? "AVAILABLE" : item.status === "BOOKED" ? "BOOKED" : "MAINTENANCE"
  })

  const formatDateText = (dateStr: string) => {
    try {
      const date = new Date(dateStr)
      return date.toLocaleDateString("vi-VN", { day: "numeric", month: "numeric" })
    } catch {
      return dateStr
    }
  }

  return (
    <div className="space-y-8 bg-white p-6 rounded-lg border shadow-sm">
      
      {/* Top Section: Header & View Toggle */}
      <div className="flex flex-col md:flex-row md:items-center justify-between border-b pb-4 gap-4">
        <div>
          <h2 className="text-xl font-bold flex items-center gap-2 text-slate-800">
            <Clock className="w-5 h-5 text-primary" />
            Lịch Sân Lẻ
          </h2>
          <p className="text-sm text-slate-500 mt-1 font-medium">
            Giờ hoạt động: <span className="text-slate-700">{openTime.substring(0, 5)} - {closeTime.substring(0, 5)}</span>
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {/* Tabs View Mode */}
          <Tabs value={viewMode} onValueChange={(val) => setViewMode(val as any)} className="w-[220px]">
            <TabsList className="grid grid-cols-2">
              <TabsTrigger value="weekly" className="flex items-center gap-1.5 text-xs font-semibold">
                <Grid className="w-3.5 h-3.5" />
                Lịch Tuần
              </TabsTrigger>
              <TabsTrigger value="list" className="flex items-center gap-1.5 text-xs font-semibold">
                <List className="w-3.5 h-3.5" />
                Danh sách
              </TabsTrigger>
            </TabsList>
          </Tabs>

          <div className="flex items-center gap-2">
            <Input
              type="number"
              placeholder="Giá mặc định..."
              value={bulkPrice}
              onChange={(e) => setBulkPrice(e.target.value)}
              className="w-28 h-9 border-slate-200"
            />
            <Button
              variant="outline"
              size="sm"
              onClick={handleBulkGenerate}
              disabled={submitting || loading}
              className="gap-1.5 border-primary/30 text-primary hover:bg-primary/5 font-semibold text-xs h-9"
            >
              {submitting ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              ) : (
                <Zap className="w-3.5 h-3.5 text-yellow-500 fill-yellow-500" />
              )}
              Tự động tạo
            </Button>
            <Button
              size="sm"
              onClick={() => setIsAddOpen(true)}
              className="gap-1.5 font-semibold text-xs h-9"
            >
              <Plus className="w-3.5 h-3.5" />
              Thêm khung giờ
            </Button>
          </div>
        </div>
      </div>

      {/* Main Workspace */}
      <div className="w-full space-y-4">
          
          {/* Weekly Grid Mode */}
          {viewMode === "weekly" && (
            <div className="space-y-4">
              {/* Navigation Header */}
              <div className="flex items-center justify-between bg-slate-50 dark:bg-muted/40 p-3 rounded-xl border border-slate-100 dark:border-border">
                <Button variant="outline" size="sm" onClick={() => navigateWeek("prev")} className="h-8">
                  <ChevronLeft className="h-4 w-4 mr-1" /> Tuần trước
                </Button>
                
                <button
                  type="button"
                  onClick={() => setCurrentDate(mondayOfThisWeek())}
                  className="flex flex-col items-center px-3 py-1 rounded-[8px] hover:bg-slate-100 transition-colors"
                >
                  <span className="text-sm font-bold text-slate-800 dark:text-slate-200 leading-tight">
                    {weeklyData ? (
                      `Tuần từ ${formatDayShort(weeklyData.weekStart)} đến ${formatDayShort(weeklyData.weekEnd)}`
                    ) : (
                      "Đang đồng bộ..."
                    )}
                  </span>
                  <span className="text-[10px] text-emerald-600 font-semibold mt-0.5">
                    Nhấn để về tuần này
                  </span>
                </button>

                <Button variant="outline" size="sm" onClick={() => navigateWeek("next")} className="h-8">
                  Tuần sau <ChevronRight className="h-4 w-4 ml-1" />
                </Button>
              </div>

              {/* Legend */}
              <div className="flex items-center gap-6 text-sm text-slate-500 font-semibold px-1 py-1">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-[#1a8a4a]" />
                  <span>Còn trống</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-[#8a1c1c]" />
                  <span>Đã đặt</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-gray-300" />
                  <span>Đã qua</span>
                </div>
              </div>

              <div className="text-xs text-blue-700 bg-blue-50 border border-blue-100 rounded-lg p-3 flex items-center gap-2 font-semibold shadow-xs">
                <span>💡</span>
                <span>Lịch tuần hiển thị trạng thái đặt sân thực tế. Bạn có thể rê chuột vào mỗi ô trống để Chỉnh sửa hoặc Xóa nhanh slot đó trực tiếp tại đây.</span>
              </div>

              {loading ? (
                <div className="flex flex-col items-center justify-center py-32 text-slate-400 bg-slate-50/50 rounded-xl border border-dashed">
                  <Loader2 className="w-10 h-10 animate-spin text-primary/40 mb-3" />
                  <p className="text-sm font-medium tracking-wide">Đang đồng bộ dữ liệu lịch tuần...</p>
                </div>
              ) : !weeklyData || weeklyData.days.length === 0 ? (
                <div className="text-center py-20 border-2 border-dashed rounded-xl bg-slate-50/30">
                  <Clock className="w-12 h-12 text-slate-200 mx-auto mb-3" />
                  <p className="text-slate-400 font-medium">Chưa cấu hình lịch hoạt động tuần này</p>
                </div>
              ) : (
                <WeeklyAgendaGrid
                  data={weeklyData}
                  emptyMessage="Chưa có khung giờ nào được thiết lập cho tuần này"
                  renderDayHeader={(day) => (
                    <>
                      <div className="text-sm font-bold text-slate-700 leading-tight">{day.dayName}</div>
                      <div className="text-[13px] text-slate-500 font-semibold leading-tight mt-1">
                        {formatDayShort(day.date)}
                      </div>
                    </>
                  )}
                  renderSlotBlock={(slot, date) => (
                    <OwnerSlotBlock
                      slot={slot}
                      onChoice={(action) => {
                        setSelectedSlotForChoice({ slot, date, action })
                        setIsChoiceOpen(true)
                      }}
                    />
                  )}
                />
              )}
            </div>
          )}

          {/* List View Mode (Original generic templates layout) */}
          {viewMode === "list" && (
            <div>
              <h3 className="text-lg font-semibold mb-4 flex items-center gap-2 text-slate-700">
                <CalendarDays className="w-4 h-4 text-slate-400" />
                Danh sách khung giờ cơ bản (Templates)
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
                          <TableCell className="text-right space-x-1.5 pr-4">
                            {slot.slotStatus !== "BOOKED" && (
                              <>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  onClick={() => {
                                    setEditingSlot(slot)
                                    setIsEditOpen(true)
                                  }}
                                  className="hover:bg-slate-100 rounded-full"
                                  title="Chỉnh sửa"
                                >
                                  <Edit3 className="w-4 h-4 text-slate-500" />
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  onClick={() => handleToggle(slot.slotId)}
                                  className="hover:bg-slate-100 rounded-full"
                                  title={slot.slotStatus === "AVAILABLE" ? "Tạm đóng" : "Mở lại"}
                                >
                                  <Switch
                                    checked={slot.slotStatus === "AVAILABLE"}
                                  />
                                </Button>
                                <Button
                                  className="text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-full transition-all"
                                  variant="ghost"
                                  size="icon"
                                  onClick={() => handleDeleteSlot(slot.slotId)}
                                >
                                  <Trash2 className="w-4.5 h-4.5" />
                                </Button>
                              </>
                            )}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </div>
          )}
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

      <EditTimeSlotDialog
        isOpen={isEditOpen}
        onClose={() => {
          setIsEditOpen(false)
          setEditingSlot(null)
          setSelectedDate(undefined)
        }}
        slot={editingSlot}
        onSuccess={loadData}
        date={selectedDate}
      />

      {/* Add Time Slot Dialog */}
      <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
        <DialogContent className="sm:max-w-[400px]">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold flex items-center gap-2 text-slate-800">
              <div className="p-1.5 bg-primary/10 rounded-lg">
                <Plus className="w-4 h-4 text-primary" />
              </div>
              Thêm khung giờ
            </DialogTitle>
          </DialogHeader>
          <form onSubmit={handleAddSlot} className="space-y-4 py-2">
            <div className="space-y-2">
              <Label htmlFor="startTime" className="text-sm font-bold text-slate-700">Giờ bắt đầu</Label>
              <Input
                id="startTime"
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                className="bg-white border-slate-200 focus:ring-primary/20"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="endTime" className="text-sm font-bold text-slate-700">Giờ kết thúc</Label>
              <Input
                id="endTime"
                type="time"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                className="bg-white border-slate-200 focus:ring-primary/20"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="price" className="text-sm font-bold text-slate-700">Giá thuê (đ)</Label>
              <Input
                id="price"
                type="number"
                placeholder="150,000"
                value={pricePerSlot}
                onChange={(e) => setPricePerSlot(e.target.value)}
                className="bg-white border-slate-200 focus:ring-primary/20"
              />
            </div>
            <DialogFooter className="pt-4 gap-2 sm:gap-0">
              <Button type="button" variant="outline" onClick={() => setIsAddOpen(false)} disabled={submitting}>
                Hủy
              </Button>
              <Button type="submit" className="gap-2" disabled={submitting}>
                {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
                Lưu khung giờ
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Choice Dialog */}
      <Dialog open={isChoiceOpen} onOpenChange={setIsChoiceOpen}>
        <DialogContent className="sm:max-w-[400px] bg-white">
          <DialogHeader>
            <DialogTitle className="text-lg font-bold text-slate-800">
              Chọn Phạm Vi Áp Dụng
            </DialogTitle>
          </DialogHeader>
          <div className="py-2">
            <p className="text-sm text-slate-600 leading-relaxed">
              Bạn muốn thực hiện thao tác này cho riêng ngày hôm nay hay tất cả các ngày (cấu hình lịch mẫu)?
            </p>
          </div>
          <div className="flex flex-col gap-2 pt-4 w-full">
            <Button
              type="button"
              onClick={handleChoiceAllDays}
              className="w-full"
            >
              Tất cả các ngày (Cấu hình lịch mẫu)
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={handleChoiceTodayOnly}
              className="w-full"
            >
              Chỉ ngày hôm nay ({selectedSlotForChoice ? formatDayShort(selectedSlotForChoice.date) : ""})
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsChoiceOpen(false)}
              className="w-full"
            >
              Hủy
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}

// ── Owner slot block (weekly agenda grid) ────────────────────────────────────

interface OwnerSlotBlockProps {
  slot: WeeklySlotItem
  onChoice: (action: "edit" | "toggle" | "delete") => void
}

function OwnerSlotBlock({ slot, onChoice }: OwnerSlotBlockProps) {
  if (slot.status === 'BOOKED') {
    return (
      <div className="h-full w-full rounded-[6px] bg-[#fdf0f0] border-[0.5px] border-[#f5b7b7] flex items-center justify-center px-3 text-[13px] font-bold text-[#8a1c1c] select-none cursor-not-allowed">
        Đã đặt
      </div>
    )
  }

  if (slot.status === 'PAST') {
    return (
      <div className="h-full w-full rounded-[6px] bg-slate-50 border-[0.5px] border-slate-200 flex items-center justify-center px-3 text-[13px] font-medium text-slate-400 select-none cursor-not-allowed">
        Đã qua
      </div>
    )
  }

  if (slot.status === 'AVAILABLE' || slot.status === 'OWNER_CLOSED') {
    const isAvailable = slot.status === 'AVAILABLE'
    return (
      <div
        className={`h-full w-full rounded-[6px] flex flex-col items-center justify-center px-3 relative group transition-colors overflow-hidden border-[0.5px] ${
          isAvailable
            ? "bg-[#e8f7ee] border-[#9eddb6] text-[#0d5c2e]"
            : "bg-orange-50 border-orange-200 text-orange-700"
        }`}
      >
        <span className="text-[13px] font-bold leading-none">
          {slot.price.toLocaleString('vi-VN')}đ
        </span>
        <span className="text-[10px] font-semibold mt-1 leading-none">
          {isAvailable ? "Mở" : "Tạm đóng"}
        </span>

        {/* Hover Actions Overlay */}
        <div className="absolute inset-0 bg-black/60 rounded-[6px] opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2 px-1.5 z-10">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => onChoice("edit")}
            className="h-7 w-7 bg-white/90 hover:bg-white text-slate-700 hover:text-primary rounded-full transition-all"
            title="Chỉnh sửa"
          >
            <Edit3 className="w-3.5 h-3.5" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => onChoice("toggle")}
            className="h-7 w-7 bg-white/90 hover:bg-white text-slate-700 hover:text-amber-600 rounded-full transition-all"
            title={isAvailable ? "Tạm đóng" : "Mở lại"}
          >
            <Power className={`w-3.5 h-3.5 ${isAvailable ? 'text-amber-600' : 'text-emerald-600'}`} />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => onChoice("delete")}
            className="h-7 w-7 bg-white/90 hover:bg-red-50 text-slate-700 hover:text-red-600 rounded-full transition-all"
            title="Xóa"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </Button>
        </div>
      </div>
    )
  }

  const label = slot.status === "MAINTENANCE" ? "Bảo trì" : "Đã qua"
  return (
    <div className="h-full w-full rounded-[6px] bg-slate-50 border-[0.5px] border-slate-200 flex items-center justify-center px-3 text-[13px] font-medium text-slate-400 select-none cursor-not-allowed">
      {label}
    </div>
  )
}
