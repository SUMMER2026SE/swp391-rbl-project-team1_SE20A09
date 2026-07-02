'use client'

import { useMemo, type ReactNode } from 'react'
import type { WeeklySlotDay, WeeklySlotItem, WeeklySlotsResponse } from '@/lib/bookings-api'

const DEFAULT_PIXELS_PER_HOUR = 90
// Đủ chỗ cho 2 dòng text (giá + nhãn trạng thái) của slot ngắn nhất mà không bị cắt.
const MIN_BLOCK_HEIGHT_PX = 36

interface WeeklyAgendaGridProps {
  data: WeeklySlotsResponse
  /** Hiển thị khi tuần không có slot nào (thay cho lưới trống). */
  emptyMessage: string
  /** Render nội dung bên trong 1 ô slot — định vị/kích thước do grid tự tính. */
  renderSlotBlock: (slot: WeeklySlotItem, date: string) => ReactNode
  renderDayHeader?: (day: WeeklySlotDay) => ReactNode
  pixelsPerHour?: number
}

/**
 * Agenda/calendar-style weekly grid — 1 trục thời gian dọc liên tục dùng chung,
 * mỗi slot định vị theo giờ bắt đầu + cao theo đúng thời lượng thực của chính nó.
 *
 * Thay cho cách gom "1 row = 1 startTime dùng chung cho cả tuần", vốn hiển thị
 * sai label khi 1 ngày có exception đổi thời lượng slot (VD "Chỉ ngày hôm nay").
 */
export default function WeeklyAgendaGrid({
  data,
  emptyMessage,
  renderSlotBlock,
  renderDayHeader = defaultDayHeader,
  pixelsPerHour = DEFAULT_PIXELS_PER_HOUR,
}: WeeklyAgendaGridProps) {
  const allSlots = useMemo(() => data.days.flatMap((day) => day.slots), [data])

  const axis = useMemo(() => {
    if (allSlots.length === 0) return null

    const pxPerMinute = pixelsPerHour / 60
    const starts = allSlots.map((s) => parseHM(s.startTime))
    const ends = allSlots.map((s) => parseHM(s.endTime))
    const axisStart = Math.floor(Math.min(...starts) / 60) * 60
    const axisEnd = Math.ceil(Math.max(...ends) / 60) * 60

    // Thước chia nhỏ xuống 30 phút nếu tuần có slot ngắn hơn 1h hoặc lệch giờ tròn —
    // chỉ ảnh hưởng độ chi tiết của thước, vị trí ô luôn tính từ giờ thực của slot.
    const needsHalfHourStep = allSlots.some((s) => {
      const start = parseHM(s.startTime)
      const end = parseHM(s.endTime)
      return end - start < 60 || start % 60 !== 0 || end % 60 !== 0
    })
    const stepMinutes = needsHalfHourStep ? 30 : 60

    const lines: { minutes: number; top: number; label: string }[] = []
    for (let m = axisStart; m <= axisEnd; m += stepMinutes) {
      lines.push({ minutes: m, top: (m - axisStart) * pxPerMinute, label: formatHM(m) })
    }

    return { start: axisStart, pxPerMinute, lines, totalHeight: (axisEnd - axisStart) * pxPerMinute }
  }, [allSlots, pixelsPerHour])

  if (!axis) {
    return (
      <div className="py-8 text-center text-[12px] text-muted-foreground">{emptyMessage}</div>
    )
  }

  return (
    <div className="overflow-x-auto -mx-1 px-1">
      <div className="min-w-[820px]">
        {/* Header ngày */}
        <div className="flex">
          <div className="w-14 shrink-0 sticky left-0 z-20 bg-background" />
          {data.days.map((day) => (
            <div key={day.date} className="flex-1 min-w-[100px] text-center px-3 pb-3 pt-2">
              {renderDayHeader(day)}
            </div>
          ))}
        </div>

        {/* Trục giờ + 7 cột ngày — cuộn dọc riêng bên trong, header ngày ở trên luôn cố định. */}
        <div className="max-h-[560px] overflow-y-auto pt-2">
          <div className="flex relative" style={{ height: axis.totalHeight }}>
            <div className="w-14 shrink-0 sticky left-0 z-20 bg-background relative">
              {axis.lines.map((line) => (
                <div
                  key={line.minutes}
                  className="absolute right-2 -translate-y-1/2 text-[11px] text-muted-foreground font-medium"
                  style={{ top: line.top }}
                >
                  {line.label}
                </div>
              ))}
            </div>

            <div className="flex-1 flex relative">
              {axis.lines.map((line) => (
                <div
                  key={line.minutes}
                  className="absolute left-0 right-0 z-0 border-t border-border/60 pointer-events-none"
                  style={{ top: line.top }}
                />
              ))}
              {data.days.map((day) => (
                <div
                  key={day.date}
                  className="flex-1 min-w-[100px] relative z-10 border-l border-border/40 first:border-l-0"
                >
                  {day.slots.map((slot) => {
                    const top = (parseHM(slot.startTime) - axis.start) * axis.pxPerMinute
                    const height = Math.max(
                      MIN_BLOCK_HEIGHT_PX,
                      (parseHM(slot.endTime) - parseHM(slot.startTime)) * axis.pxPerMinute
                    )
                    return (
                      <div key={slot.slotId} className="absolute inset-x-0.5" style={{ top, height }}>
                        {renderSlotBlock(slot, day.date)}
                      </div>
                    )
                  })}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function defaultDayHeader(day: WeeklySlotDay) {
  return (
    <>
      <div className="text-sm font-semibold text-foreground leading-tight">{day.dayName}</div>
      <div className="text-[13px] text-muted-foreground font-medium leading-tight mt-1">
        {formatDayShort(day.date)}
      </div>
    </>
  )
}

function parseHM(hm: string): number {
  const [h, m] = hm.split(':').map(Number)
  return h * 60 + m
}

function formatHM(totalMinutes: number): string {
  const h = Math.floor(totalMinutes / 60)
  const m = totalMinutes % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

function formatDayShort(ymd: string): string {
  const [, m, d] = ymd.split('-')
  return `${d}/${m}`
}
