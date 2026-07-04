'use client'

import { useEffect, useState } from 'react'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import { Badge } from '@/components/ui/badge'
import { Calendar } from '@/components/ui/calendar'
import { Loader2, Wrench, Info } from 'lucide-react'
import { format, parseISO } from 'date-fns'
import { stadiumService } from '@/lib/services/stadium'
import { MaintenanceScheduleResponse } from '@/types/stadium'
import { toast } from 'sonner'

interface MaintenanceScheduleDialogProps {
  isOpen: boolean
  onClose: () => void
  targetType: 'stadium' | 'complex'
  targetId: number
  targetName: string
  /** Gọi sau khi tạo/kết thúc lịch bảo trì thành công — dùng để trang cha refetch danh sách sân/tổ hợp (stadiumStatus, underMaintenanceToday...). */
  onSuccess?: () => void
}

export function MaintenanceScheduleDialog({ isOpen, onClose, targetType, targetId, targetName, onSuccess }: MaintenanceScheduleDialogProps) {
  const [startDate, setStartDate] = useState<Date | undefined>(undefined)
  const [endDate, setEndDate] = useState<Date | undefined>(undefined)
  const [indefinite, setIndefinite] = useState(false)
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const [hourScoped, setHourScoped] = useState(false)
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')

  const [schedules, setSchedules] = useState<MaintenanceScheduleResponse[]>([])
  const [loadingSchedules, setLoadingSchedules] = useState(false)
  const [endingId, setEndingId] = useState<number | null>(null)

  const loadSchedules = (cancelled?: { current: boolean }) => {
    setLoadingSchedules(true)
    const request = targetType === 'complex'
      ? stadiumService.listComplexMaintenanceSchedules(targetId)
      : stadiumService.listMaintenanceSchedules(targetId)
    request
      .then((data) => {
        if (!cancelled?.current) setSchedules(data)
      })
      .catch(() => {
        if (!cancelled?.current) toast.error('Không thể tải lịch sử bảo trì')
      })
      .finally(() => {
        if (!cancelled?.current) setLoadingSchedules(false)
      })
  }

  useEffect(() => {
    const cancelled = { current: false }
    if (isOpen) {
      setStartDate(undefined)
      setEndDate(undefined)
      setIndefinite(false)
      setReason('')
      setHourScoped(false)
      setStartTime('')
      setEndTime('')
      loadSchedules(cancelled)
    }
    return () => {
      cancelled.current = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen, targetType, targetId])

  // Vô thời hạn chỉ cần 1 ngày bắt đầu — đổi mode lịch sang chọn 1 ngày, bỏ ngày kết thúc đã chọn (nếu có).
  const handleIndefiniteChange = (checked: boolean) => {
    setIndefinite(checked)
    setEndDate(undefined)
    if (checked) setEndTime('')
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!startDate) {
      toast.error('Vui lòng chọn ngày bắt đầu bảo trì')
      return
    }
    if (!indefinite && !endDate) {
      toast.error('Vui lòng chọn ngày kết thúc, hoặc tick "Bảo trì vô thời hạn"')
      return
    }
    if (hourScoped && !startTime) {
      toast.error('Vui lòng chọn giờ bắt đầu bảo trì')
      return
    }
    if (hourScoped && !indefinite && !endTime) {
      toast.error('Vui lòng chọn giờ kết thúc bảo trì')
      return
    }

    setSubmitting(true)
    try {
      const payload = {
        startDate: format(startDate, 'yyyy-MM-dd'),
        endDate: indefinite ? undefined : format(endDate as Date, 'yyyy-MM-dd'),
        startTime: hourScoped && startTime ? `${startTime}:00` : undefined,
        endTime: hourScoped && !indefinite && endTime ? `${endTime}:00` : undefined,
        reason: reason.trim() || undefined,
      }
      if (targetType === 'complex') {
        await stadiumService.createComplexMaintenanceSchedule(targetId, payload)
      } else {
        await stadiumService.createMaintenanceSchedule(targetId, payload)
      }
      toast.success('Đã đặt lịch bảo trì thành công.')
      setStartDate(undefined)
      setEndDate(undefined)
      setIndefinite(false)
      setReason('')
      setHourScoped(false)
      setStartTime('')
      setEndTime('')
      loadSchedules()
      onSuccess?.()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Đã xảy ra lỗi khi đặt lịch bảo trì'
      toast.error(msg)
    } finally {
      setSubmitting(false)
    }
  }

  const handleEnd = async (maintenanceId: number) => {
    setEndingId(maintenanceId)
    try {
      await stadiumService.endMaintenanceSchedule(maintenanceId)
      toast.success('Đã kết thúc lịch bảo trì.')
      loadSchedules()
      onSuccess?.()
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể kết thúc lịch bảo trì'
      toast.error(msg)
    } finally {
      setEndingId(null)
    }
  }

  const todayStr = format(new Date(), 'yyyy-MM-dd')

  const scheduleBadge = (s: MaintenanceScheduleResponse) => {
    if (s.active) return <Badge variant="destructive">Đang bảo trì</Badge>
    if (s.endDate && s.endDate < todayStr) return <Badge variant="outline" className="text-muted-foreground">Đã kết thúc</Badge>
    return <Badge variant="secondary">Sắp tới</Badge>
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[560px] max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold flex items-center gap-2">
            <Wrench className="h-5 w-5 text-primary" />
            Đặt lịch bảo trì — {targetName}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-5 py-2">
          <div className="flex items-center space-x-2">
            <Checkbox
              id="maintenance-indefinite"
              checked={indefinite}
              onCheckedChange={(checked) => handleIndefiniteChange(checked === true)}
              disabled={submitting}
            />
            <Label htmlFor="maintenance-indefinite" className="text-sm font-semibold cursor-pointer select-none">
              Bảo trì vô thời hạn (chưa xác định ngày mở lại)
            </Label>
          </div>

          <div className="space-y-2">
            <Label className="text-sm font-bold">
              {indefinite ? 'Chọn ngày bắt đầu bảo trì' : 'Chọn khung ngày bảo trì'}
            </Label>
            <div className="border rounded-lg p-2 flex justify-center bg-muted/20">
              {indefinite ? (
                <Calendar
                  mode="single"
                  selected={startDate}
                  onSelect={setStartDate}
                  disabled={{ before: new Date() }}
                />
              ) : (
                <Calendar
                  mode="range"
                  selected={{ from: startDate, to: endDate }}
                  onSelect={(r) => {
                    setStartDate(r?.from)
                    setEndDate(r?.to)
                  }}
                  disabled={{ before: new Date() }}
                />
              )}
            </div>
            {startDate && (
              <p className="text-xs text-muted-foreground">
                Từ {format(startDate, 'dd/MM/yyyy')}
                {indefinite ? ' — Vô thời hạn' : endDate ? ` đến ${format(endDate, 'dd/MM/yyyy')}` : ' (chưa chọn ngày kết thúc)'}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <div className="flex items-center space-x-2">
              <Checkbox
                id="maintenance-hour-scoped"
                checked={hourScoped}
                onCheckedChange={(checked) => setHourScoped(checked === true)}
                disabled={submitting}
              />
              <Label htmlFor="maintenance-hour-scoped" className="text-sm font-semibold cursor-pointer select-none">
                Chỉ bảo trì theo khung giờ cụ thể (không phải cả ngày)
              </Label>
            </div>
            {hourScoped && (
              <div className="grid grid-cols-2 gap-3 pt-1">
                <div className="space-y-1">
                  <Label htmlFor="maintenance-start-time" className="text-xs">Giờ bắt đầu</Label>
                  <Input
                    id="maintenance-start-time"
                    type="time"
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    disabled={submitting}
                  />
                </div>
                {!indefinite && (
                  <div className="space-y-1">
                    <Label htmlFor="maintenance-end-time" className="text-xs">Giờ kết thúc</Label>
                    <Input
                      id="maintenance-end-time"
                      type="time"
                      value={endTime}
                      onChange={(e) => setEndTime(e.target.value)}
                      disabled={submitting}
                    />
                  </div>
                )}
              </div>
            )}
            {hourScoped && (
              <p className="text-xs text-muted-foreground">
                Ví dụ: bảo trì từ 14:00 hôm nay đến 09:00 ngày hôm sau, hoặc chỉ 10:00-12:00 trong ngày.
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="maintenance-reason">Lý do bảo trì</Label>
            <Textarea
              id="maintenance-reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              disabled={submitting}
              placeholder="VD: Sửa mặt sân, thay lưới..."
              rows={2}
              maxLength={255}
            />
          </div>

          <DialogFooter className="pt-2 border-t">
            <Button type="button" variant="outline" onClick={onClose} disabled={submitting}>
              Đóng
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Tạo lịch bảo trì
            </Button>
          </DialogFooter>
        </form>

        <div className="border-t pt-4 space-y-2">
          <Label className="text-sm font-bold">Lịch sử bảo trì</Label>
          {loadingSchedules ? (
            <div className="flex justify-center py-6">
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          ) : schedules.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-6 text-center">
              <Info className="h-5 w-5 text-muted-foreground mb-1.5" />
              <p className="text-muted-foreground text-xs">Chưa có lịch bảo trì nào</p>
            </div>
          ) : (
            <div className="space-y-2 max-h-52 overflow-y-auto">
              {schedules.map((s) => {
                const isEnded = s.endDate !== null && s.endDate < todayStr
                return (
                  <div key={s.maintenanceId} className="flex items-center justify-between border rounded-lg p-2.5 text-xs gap-2">
                    <div className="space-y-0.5 min-w-0">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        {scheduleBadge(s)}
                        <span className="font-semibold">
                          {format(parseISO(s.startDate), 'dd/MM/yyyy')}
                          {' → '}
                          {s.indefinite ? 'Vô thời hạn' : s.endDate ? format(parseISO(s.endDate), 'dd/MM/yyyy') : ''}
                        </span>
                      </div>
                      {(s.startTime || s.endTime) && (
                        <p className="text-muted-foreground">
                          Khung giờ: {s.startTime ? s.startTime.slice(0, 5) : '00:00'}
                          {' - '}
                          {s.endTime ? s.endTime.slice(0, 5) : '24:00'}
                        </p>
                      )}
                      {s.reason && <p className="text-muted-foreground truncate">{s.reason}</p>}
                    </div>
                    {!isEnded && (
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive hover:text-destructive h-7 text-xs shrink-0"
                        onClick={() => handleEnd(s.maintenanceId)}
                        disabled={endingId === s.maintenanceId}
                      >
                        {endingId === s.maintenanceId && <Loader2 className="mr-1 h-3 w-3 animate-spin" />}
                        Kết thúc
                      </Button>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
