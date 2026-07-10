'use client'

import { useEffect, useState } from 'react'
import { IconChevronLeft, IconChevronRight } from '@tabler/icons-react'
import {
  getWeeklySlots,
  type WeeklySlotItem,
  type WeeklySlotsResponse,
} from '@/lib/bookings-api'
import WeeklyAgendaGrid from './WeeklyAgendaGrid'

interface WeeklyScheduleProps {
  stadiumId: number
  /** Callback khi user click một slot AVAILABLE — parent dùng để đặt sân. */
  onSlotSelect: (slotId: number, date: string, startTime?: string, price?: number, endTime?: string) => void
}

/**
 * UC-CUS-01: Weekly grid cho trang chi tiết sân — 7 ngày × N khung giờ.
 *
 * <p>Layout:</p>
 * <ul>
 *   <li>Header: prev/next tuần + label {@code DD/MM – DD/MM}.</li>
 *   <li>Bảng: cột đầu là khung giờ, 7 cột tiếp theo là 7 ngày (thứ 2 → chủ nhật).</li>
 *   <li>Mỗi ô: AVAILABLE (xanh, hiển thị giá) / BOOKED (đỏ, "Đã đặt") / PAST (xám, "Đã qua").</li>
 * </ul>
 *
 * <p>State local: tuần đang xem + fetch. Việc đặt sân thuộc về parent
 * (VenueDetail) để tận dụng booking CTA + auth gate hiện có.</p>
 */
export default function WeeklySchedule({ stadiumId, onSlotSelect }: WeeklyScheduleProps) {
  const [weekStart, setWeekStart] = useState<string>(() => mondayOfThisWeek())
  const [data, setData] = useState<WeeklySlotsResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [])

  useEffect(() => {
    let cancelled = false
    async function fetchWeek() {
      try {
        setLoading(true)
        const res = await getWeeklySlots(stadiumId, weekStart)
        if (!cancelled) setData(res)
      } catch {
        if (!cancelled) setData(null)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    fetchWeek()
    return () => {
      cancelled = true
    }
  }, [stadiumId, weekStart])

  const handlePrevWeek = () => setWeekStart(addDays(weekStart, -7))
  const handleNextWeek = () => setWeekStart(addDays(weekStart, 7))
  const handleThisWeek = () => setWeekStart(mondayOfThisWeek())

  const weekEnd = data?.weekEnd ?? addDays(weekStart, 6)

  return (
    <div className="flex flex-col gap-3">
      {/* Week navigation header */}
      <div className="flex items-center justify-between gap-2">
        <button
          onClick={handlePrevWeek}
          type="button"
          aria-label="Tuần trước"
          className="w-8 h-8 rounded-full flex items-center justify-center border-[0.5px] border-gray-200 hover:bg-gray-50 transition-colors"
        >
          <IconChevronLeft className="w-4 h-4 text-gray-600" />
        </button>

        <button
          onClick={handleThisWeek}
          type="button"
          className="flex flex-col items-center px-3 py-1 rounded-[8px] hover:bg-gray-50 transition-colors"
          aria-label="Về tuần hiện tại"
        >
          <span className="text-[13px] font-medium text-gray-700 leading-tight">
            {formatRangeLabel(weekStart, weekEnd)}
          </span>
          <span className="text-[10px] text-[#1a8a4a] font-medium mt-0.5">
            Nhấn để về tuần này
          </span>
        </button>

        <button
          onClick={handleNextWeek}
          type="button"
          aria-label="Tuần sau"
          className="w-8 h-8 rounded-full flex items-center justify-center border-[0.5px] border-gray-200 hover:bg-gray-50 transition-colors"
        >
          <IconChevronRight className="w-4 h-4 text-gray-600" />
        </button>
      </div>

      {/* Legend */}
      <div className="flex items-center gap-6 text-sm text-gray-500 font-medium px-1 py-1">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-[#1a8a4a]" />
          <span>Còn trống</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-[#8a1c1c]" />
          <span>Đã đặt</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-amber-400" />
          <span>Đang giữ chỗ</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-gray-300" />
          <span>Đã qua</span>
        </div>
      </div>

      {/* Grid */}
      {loading && !data ? (
        <div className="py-8 text-center text-[12px] text-gray-400">
          Đang tải lịch tuần…
        </div>
      ) : !data ? (
        <div className="py-8 text-center text-[12px] text-gray-400">
          Sân chưa có khung giờ nào khả dụng cho tuần này.
        </div>
      ) : (
        <WeeklyAgendaGrid
          data={data}
          emptyMessage="Sân chưa có khung giờ nào khả dụng cho tuần này."
          renderSlotBlock={(slot, date) => (
            <SlotCell slot={slot} date={date} now={now} onPick={onSlotSelect} />
          )}
        />
      )}
    </div>
  )
}

// ── Cell ─────────────────────────────────────────────────────────────────────

interface SlotCellProps {
  slot: WeeklySlotItem
  date: string
  now: number
  onPick: (slotId: number, date: string, startTime: string, price: number, endTime: string) => void
}

function SlotCell({ slot, date, now, onPick }: SlotCellProps) {
  if (slot.status === 'BOOKED') {
    return (
      <div
        aria-disabled
        className="h-full w-full rounded-[6px] bg-[#fdf0f0] border-[0.5px] border-[#f5b7b7] flex items-center justify-center px-3 text-[13px] font-medium text-[#8a1c1c] select-none cursor-not-allowed"
      >
        Đã đặt
      </div>
    )
  }

  if (slot.status === 'HELD') {
    const remainingSeconds = slot.heldUntil
      ? Math.max(0, Math.ceil((new Date(slot.heldUntil).getTime() - now) / 1000))
      : null
    const countdown = remainingSeconds === null
      ? null
      : `${Math.floor(remainingSeconds / 60)}:${String(remainingSeconds % 60).padStart(2, '0')}`
    return (
      <div
        aria-disabled
        title="Đơn đang chờ thanh toán; slot sẽ tự mở lại khi hết thời gian giữ chỗ."
        className="h-full w-full rounded-[6px] bg-amber-50 border-[0.5px] border-amber-300 flex flex-col items-center justify-center px-2 text-[12px] font-medium text-amber-800 select-none cursor-not-allowed leading-tight"
      >
        <span>Đang giữ chỗ</span>
        {countdown && <span className="font-semibold tabular-nums">{countdown}</span>}
      </div>
    )
  }

  if (slot.status === 'OWNER_CLOSED') {
    return (
      <div
        aria-disabled
        className="h-full w-full rounded-[6px] bg-[#fff7ed] border-[0.5px] border-[#ffedd5] flex items-center justify-center px-3 text-[13px] font-medium text-[#c2410c] select-none cursor-not-allowed"
      >
        Tạm đóng
      </div>
    )
  }

  if (slot.status === 'MAINTENANCE') {
    return (
      <div
        aria-disabled
        className="h-full w-full rounded-[6px] bg-[#f3f4f6] border-[0.5px] border-[#e5e7eb] flex items-center justify-center px-3 text-[13px] font-medium text-gray-500 select-none cursor-not-allowed"
      >
        Bảo trì
      </div>
    )
  }

  if (slot.status === 'PAST') {
    return (
      <div
        aria-disabled
        className="h-full w-full rounded-[6px] bg-gray-50 border-[0.5px] border-gray-200 flex items-center justify-center px-3 text-[13px] font-medium text-gray-400 select-none cursor-not-allowed"
      >
        Đã qua
      </div>
    )
  }

  // AVAILABLE
  return (
    <button
      type="button"
      onClick={() => onPick(slot.slotId, date, slot.startTime, slot.price, slot.endTime)}
      className="h-full w-full rounded-[6px] bg-[#e8f7ee] border-[0.5px] border-[#9eddb6] flex flex-col items-center justify-center px-3 cursor-pointer hover:bg-[#d4f0e2] active:bg-[#c0e8d2] transition-colors group"
      aria-label={`Đặt slot ${slot.startTime}-${slot.endTime} ngày ${date}`}
    >
      <span className="text-[13px] font-semibold text-[#0d5c2e] leading-none">
        {slot.price.toLocaleString('vi-VN')}đ
      </span>
      <span className="text-[11px] text-[#1a8a4a] font-medium mt-1 leading-none opacity-0 group-hover:opacity-100 transition-opacity">
        Đặt ngay
      </span>
    </button>
  )
}

// ── Date helpers ─────────────────────────────────────────────────────────────

function mondayOfThisWeek(): string {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  // JS getDay(): Sunday = 0, Monday = 1, ..., Saturday = 6
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
  const d = parseYMD(ymd)
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`
}

function formatRangeLabel(start: string, end: string): string {
  return `${formatDayShort(start)} – ${formatDayShort(end)}`
}
