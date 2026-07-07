'use client'

import { useState, useEffect, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import { useSession, signIn } from 'next-auth/react'
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import {
  getComplexFacilities,
  getFacilityCourts,
  getComplexCourts,
} from '@/lib/api/complex'
import { getSlotsByDate } from '@/lib/bookings-api'
import type { FacilityDto, CourtWithSlotsDto } from '@/types/complex'
import type { SlotAvailability } from '@/lib/bookings-api'
import {
  CalendarDays,
  ChevronLeft,
  MapPin,
  Clock,
  CheckCircle2,
  LogIn,
  AlertCircle,
  Loader2,
} from 'lucide-react'
import { toast } from 'sonner'

// ── Helpers ──────────────────────────────────────────────────────────────────

const PENDING_KEY = 'pendingBooking'
const PENDING_TTL_MS = 10 * 60 * 1000 // 10 minutes

interface PendingBooking {
  complexId: number
  courtId: number
  slotId: number
  date: string
  slotLabel: string
  expiredAt: number
}

function savePending(data: Omit<PendingBooking, 'expiredAt'>) {
  try {
    sessionStorage.setItem(
      PENDING_KEY,
      JSON.stringify({ ...data, expiredAt: Date.now() + PENDING_TTL_MS })
    )
  } catch {}
}

function buildBookingUrl(courtId: number, slotId: number, date: string) {
  return `/booking/new?stadiumId=${courtId}&slotId=${slotId}&date=${encodeURIComponent(date)}`
}

function fmt(t: string) {
  return t?.substring(0, 5) ?? ''
}

function formatPrice(n: number) {
  return n.toLocaleString('vi-VN') + 'đ/h'
}

// Next 7 days (today included)
function getNext7Days() {
  const days: { iso: string; label: string; dayName: string }[] = []
  const locale = 'vi-VN'
  for (let i = 0; i < 7; i++) {
    const d = new Date()
    d.setDate(d.getDate() + i)
    const iso = d.toISOString().split('T')[0]
    const label = d.toLocaleDateString(locale, { day: '2-digit', month: '2-digit' })
    const dayName =
      i === 0
        ? 'Hôm nay'
        : i === 1
        ? 'Ngày mai'
        : d.toLocaleDateString(locale, { weekday: 'short' })
    days.push({ iso, label, dayName })
  }
  return days
}

// ── Sub-components ─────────────────────────────────────────────────────────

interface LoginModalProps {
  onLogin: () => void
  onCancel: () => void
}

function LoginModal({ onLogin, onCancel }: LoginModalProps) {
  return (
    <div className="fixed inset-0 z-[60] flex items-end sm:items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="bg-white rounded-2xl w-full max-w-sm p-6 shadow-2xl flex flex-col gap-5 animate-in slide-in-from-bottom-4 duration-300">
        <div className="flex flex-col items-center gap-3 text-center">
          <div className="w-14 h-14 rounded-full bg-emerald-50 flex items-center justify-center">
            <LogIn className="w-7 h-7 text-emerald-600" />
          </div>
          <div>
            <h3 className="text-[16px] font-bold text-gray-900">Đăng nhập để hoàn tất</h3>
            <p className="text-[13px] text-gray-500 mt-1">
              Vui lòng đăng nhập để xác nhận đặt sân. Lựa chọn của bạn sẽ được lưu lại.
            </p>
          </div>
        </div>
        <Button
          onClick={onLogin}
          className="w-full rounded-xl py-5 font-bold bg-emerald-600 hover:bg-emerald-700 text-white"
        >
          <LogIn className="w-4 h-4 mr-2" />
          Đăng nhập ngay
        </Button>
        <button
          type="button"
          onClick={onCancel}
          className="text-[13px] text-gray-400 hover:text-gray-600 text-center transition-colors"
        >
          Quay lại chọn slot
        </button>
      </div>
    </div>
  )
}

// ── Main Drawer ────────────────────────────────────────────────────────────

interface QuickBookDrawerProps {
  complexId: number
  complexName: string
  complexAddress: string
  open: boolean
  onClose: () => void
}

interface DrawerState {
  step: 1 | 2
  selectedCourtId: number | null
  selectedCourtName: string
  selectedDate: string
  selectedSlotId: number | null
  selectedSlotLabel: string
  pricePerHour: number | null
}

const INITIAL_STATE: DrawerState = {
  step: 1,
  selectedCourtId: null,
  selectedCourtName: '',
  selectedDate: new Date().toISOString().split('T')[0],
  selectedSlotId: null,
  selectedSlotLabel: '',
  pricePerHour: null,
}

export default function QuickBookDrawer({
  complexId,
  complexName,
  complexAddress,
  open,
  onClose,
}: QuickBookDrawerProps) {
  const router = useRouter()
  const { data: session } = useSession()

  // Drawer state
  const [state, setState] = useState<DrawerState>(INITIAL_STATE)

  // Data
  const [facilities, setFacilities] = useState<FacilityDto[]>([])
  const [courts, setCourts] = useState<CourtWithSlotsDto[]>([])
  const [slots, setSlots] = useState<SlotAvailability[]>([])

  // Loading / error
  const [loadingCourts, setLoadingCourts] = useState(false)
  const [loadingSlots, setLoadingSlots] = useState(false)

  // Auth modal
  const [showLoginModal, setShowLoginModal] = useState(false)

  const days = getNext7Days()

  // ── Reset when drawer opens ──────────────────────────────────────────────
  useEffect(() => {
    if (open) {
      setState(INITIAL_STATE)
      setFacilities([])
      setCourts([])
      setSlots([])
      setShowLoginModal(false)
      // Fetch facilities/courts
      fetchCourts()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  // ── Fetch courts (flat or via facilities) ────────────────────────────────
  const fetchCourts = useCallback(async () => {
    setLoadingCourts(true)
    try {
      // Try facilities first
      const facs = await getComplexFacilities(complexId)
      if (facs && facs.length > 0) {
        setFacilities(facs)
        // Fetch courts for first available facility
        const firstAvailable = facs.find(f => f.stadiumStatus !== 'CLOSED') ?? facs[0]
        const c = await getFacilityCourts(firstAvailable.stadiumId)
        setCourts(c)
      } else {
        // No facilities → fetch courts directly
        const c = await getComplexCourts(complexId)
        setCourts(c)
      }
    } catch {
      toast.error('Không thể tải danh sách sân. Vui lòng thử lại.')
    } finally {
      setLoadingCourts(false)
    }
  }, [complexId])

  // ── Fetch courts for selected facility ──────────────────────────────────
  const handleFacilityChange = async (facilityId: number) => {
    setLoadingCourts(true)
    setState(prev => ({ ...prev, selectedCourtId: null, selectedSlotId: null, selectedSlotLabel: '' }))
    try {
      const c = await getFacilityCourts(facilityId)
      setCourts(c)
    } catch {
      toast.error('Không thể tải sân lẻ.')
    } finally {
      setLoadingCourts(false)
    }
  }

  // ── Select court → go step 2 ─────────────────────────────────────────────
  const handleSelectCourt = (court: CourtWithSlotsDto) => {
    setState(prev => ({
      ...prev,
      step: 2,
      selectedCourtId: court.stadiumId,
      selectedCourtName: court.stadiumName,
      pricePerHour: court.pricePerHour,
      selectedSlotId: null,
      selectedSlotLabel: '',
    }))
  }

  // ── Fetch slots when date or court changes ───────────────────────────────
  useEffect(() => {
    if (state.step !== 2 || !state.selectedCourtId || !state.selectedDate) return
    setLoadingSlots(true)
    setSlots([])
    setState(prev => ({ ...prev, selectedSlotId: null, selectedSlotLabel: '' }))
    getSlotsByDate(state.selectedCourtId, state.selectedDate)
      .then(setSlots)
      .catch(() => toast.error('Không thể tải khung giờ.'))
      .finally(() => setLoadingSlots(false))
  }, [state.step, state.selectedCourtId, state.selectedDate])

  // ── Confirm booking ──────────────────────────────────────────────────────
  const handleConfirm = () => {
    if (!state.selectedCourtId || !state.selectedSlotId || !state.selectedDate) return

    const url = buildBookingUrl(state.selectedCourtId, state.selectedSlotId, state.selectedDate)

    if (!session) {
      // Save pending booking
      savePending({
        complexId,
        courtId: state.selectedCourtId,
        slotId: state.selectedSlotId,
        date: state.selectedDate,
        slotLabel: state.selectedSlotLabel,
      })
      setShowLoginModal(true)
      return
    }
    onClose()
    router.push(url)
  }

  // ── Login via modal ──────────────────────────────────────────────────────
  const handleLogin = () => {
    if (!state.selectedCourtId || !state.selectedSlotId) return
    const callbackUrl = buildBookingUrl(
      state.selectedCourtId,
      state.selectedSlotId,
      state.selectedDate
    )
    signIn(undefined, { callbackUrl })
  }

  const canConfirm = !!state.selectedCourtId && !!state.selectedSlotId

  return (
    <>
      <Sheet open={open} onOpenChange={(o) => !o && onClose()}>
        <SheetContent
          side="right"
          className="w-full sm:max-w-[480px] p-0 flex flex-col gap-0 overflow-hidden"
        >
          {/* ── HEADER ───────────────────────────────────────────────── */}
          <SheetHeader className="px-5 py-4 border-b border-gray-100 shrink-0">
            <div className="flex items-center gap-3">
              {state.step === 2 && (
                <button
                  type="button"
                  onClick={() => setState(prev => ({ ...prev, step: 1, selectedSlotId: null, selectedSlotLabel: '' }))}
                  className="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 flex items-center justify-center transition-colors shrink-0"
                  aria-label="Quay lại"
                >
                  <ChevronLeft className="w-4 h-4 text-gray-600" />
                </button>
              )}
              <div className="min-w-0 flex-1">
                <SheetTitle className="text-[15px] font-bold text-gray-900 truncate leading-tight">
                  {state.step === 1 ? 'Chọn sân lẻ' : state.selectedCourtName}
                </SheetTitle>
                <SheetDescription className="text-[12px] text-gray-400 truncate mt-0.5">
                  <MapPin className="w-3 h-3 inline mr-1" />
                  {complexName} · {complexAddress}
                </SheetDescription>
              </div>
            </div>

            {/* Step indicator */}
            <div className="flex gap-1.5 mt-3">
              {([1, 2] as const).map(s => (
                <div
                  key={s}
                  className={[
                    'h-1 flex-1 rounded-full transition-all duration-300',
                    state.step >= s ? 'bg-emerald-500' : 'bg-gray-100',
                  ].join(' ')}
                />
              ))}
            </div>
          </SheetHeader>

          {/* ── BODY ─────────────────────────────────────────────────── */}
          <div className="flex-1 overflow-y-auto">

            {/* STEP 1: Court list */}
            {state.step === 1 && (
              <div className="p-5 flex flex-col gap-5">
                {/* Facility tabs (if available) */}
                {facilities.length > 1 && (
                  <div>
                    <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-2">
                      Chọn khu vực
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {facilities.map(f => {
                        const disabled = f.stadiumStatus === 'CLOSED'
                        return (
                          <button
                            key={f.stadiumId}
                            type="button"
                            disabled={disabled}
                            onClick={() => handleFacilityChange(f.stadiumId)}
                            className={[
                              'px-3 py-1.5 rounded-lg text-[12px] font-semibold border transition-all',
                              disabled
                                ? 'opacity-40 cursor-not-allowed bg-gray-50 border-gray-100 text-gray-400'
                                : 'bg-white border-gray-200 text-gray-700 hover:border-emerald-500 hover:bg-emerald-50 hover:text-emerald-700',
                            ].join(' ')}
                          >
                            {f.stadiumName}
                          </button>
                        )
                      })}
                    </div>
                  </div>
                )}

                {/* Courts */}
                <div>
                  <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-3">
                    Sân lẻ ({courts.length})
                  </p>

                  {loadingCourts ? (
                    <div className="flex flex-col gap-3">
                      {[1, 2, 3].map(i => (
                        <div key={i} className="h-20 bg-gray-100 rounded-xl animate-pulse" />
                      ))}
                    </div>
                  ) : courts.length === 0 ? (
                    <div className="text-center py-10 text-gray-400 text-[13px]">
                      Không có sân nào khả dụng.
                    </div>
                  ) : (
                    <div className="flex flex-col gap-3">
                      {courts.map(court => {
                        const available = court.stadiumStatus === 'AVAILABLE' && !court.underMaintenanceToday
                        const maintenance = court.stadiumStatus === 'MAINTENANCE' || (court.stadiumStatus === 'AVAILABLE' && !!court.underMaintenanceToday)
                        const closed = court.stadiumStatus === 'CLOSED'
                        const disabled = closed

                        return (
                          <button
                            key={court.stadiumId}
                            type="button"
                            disabled={disabled}
                            onClick={() => !disabled && handleSelectCourt(court)}
                            className={[
                              'w-full text-left p-4 rounded-xl border transition-all duration-200',
                              'flex items-center justify-between gap-4',
                              disabled
                                ? 'opacity-50 cursor-not-allowed bg-gray-50 border-gray-100'
                                : 'bg-white border-gray-200 hover:border-emerald-400 hover:bg-emerald-50/50 hover:shadow-sm cursor-pointer',
                            ].join(' ')}
                          >
                            <div className="flex-1 min-w-0">
                              <p className="font-bold text-[14px] text-gray-900 truncate">
                                {court.stadiumName}
                              </p>
                              <div className="flex items-center gap-2 mt-1">
                                <span className="text-[12px] text-emerald-600 font-semibold">
                                  {formatPrice(court.pricePerHour)}
                                </span>
                                <span
                                  className={[
                                    'text-[10px] font-bold px-2 py-0.5 rounded-full',
                                    available ? 'bg-emerald-50 text-emerald-700' :
                                    maintenance ? 'bg-amber-50 text-amber-700' :
                                    'bg-red-50 text-red-700',
                                  ].join(' ')}
                                >
                                  {available ? 'Còn trống' : maintenance ? 'Bảo trì' : 'Đóng'}
                                </span>
                              </div>
                              {court.description && (
                                <p className="text-[11px] text-gray-400 mt-1 line-clamp-1">
                                  {court.description}
                                </p>
                              )}
                            </div>
                            {!disabled && (
                              <div className="shrink-0 w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600">
                                <CalendarDays className="w-4 h-4" />
                              </div>
                            )}
                          </button>
                        )
                      })}
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* STEP 2: Date + Slot */}
            {state.step === 2 && (
              <div className="p-5 flex flex-col gap-5">
                {/* Date picker */}
                <div>
                  <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-3">
                    Chọn ngày
                  </p>
                  <div className="flex gap-2 overflow-x-auto pb-1 -mx-1 px-1 scrollbar-none">
                    {days.map(day => {
                      const active = state.selectedDate === day.iso
                      return (
                        <button
                          key={day.iso}
                          type="button"
                          onClick={() => setState(prev => ({ ...prev, selectedDate: day.iso, selectedSlotId: null, selectedSlotLabel: '' }))}
                          className={[
                            'flex flex-col items-center px-3 py-2 rounded-xl border min-w-[60px] transition-all duration-200 shrink-0',
                            active
                              ? 'bg-emerald-600 border-emerald-600 text-white shadow-md shadow-emerald-200'
                              : 'bg-white border-gray-200 text-gray-700 hover:border-emerald-400',
                          ].join(' ')}
                        >
                          <span className={`text-[10px] font-semibold ${active ? 'text-emerald-100' : 'text-gray-400'}`}>
                            {day.dayName}
                          </span>
                          <span className="text-[14px] font-bold mt-0.5">{day.label}</span>
                        </button>
                      )
                    })}
                  </div>
                </div>

                {/* Slot grid */}
                <div>
                  <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-3">
                    Chọn khung giờ
                  </p>

                  {loadingSlots ? (
                    <div className="grid grid-cols-3 gap-2">
                      {[1, 2, 3, 4, 5, 6].map(i => (
                        <div key={i} className="h-14 bg-gray-100 rounded-xl animate-pulse" />
                      ))}
                    </div>
                  ) : slots.length === 0 ? (
                    <div className="flex flex-col items-center py-10 gap-2 text-gray-400">
                      <Clock className="w-8 h-8" />
                      <p className="text-[13px]">Không có khung giờ nào trong ngày này.</p>
                    </div>
                  ) : (
                    <div className="grid grid-cols-3 gap-2">
                      {slots.map(slot => {
                        const isAvailable = slot.available
                        const isSelected = state.selectedSlotId === slot.slotId
                        const label = `${fmt(slot.startTime)} – ${fmt(slot.endTime)}`

                        return (
                          <button
                            key={slot.slotId}
                            type="button"
                            disabled={!isAvailable}
                            onClick={() =>
                              isAvailable &&
                              setState(prev => ({
                                ...prev,
                                selectedSlotId: slot.slotId,
                                selectedSlotLabel: label,
                              }))
                            }
                            className={[
                              'flex flex-col items-center justify-center py-2.5 px-2 rounded-xl border text-center transition-all duration-150',
                              !isAvailable
                                ? 'opacity-40 cursor-not-allowed bg-gray-50 border-gray-100'
                                : isSelected
                                ? 'bg-emerald-600 border-emerald-600 text-white shadow-md shadow-emerald-200'
                                : 'bg-white border-gray-200 hover:border-emerald-400 hover:bg-emerald-50 cursor-pointer',
                            ].join(' ')}
                          >
                            <span className={`text-[12px] font-bold ${isSelected ? 'text-white' : 'text-gray-800'}`}>
                              {fmt(slot.startTime)}
                            </span>
                            <span className={`text-[10px] mt-0.5 ${isSelected ? 'text-emerald-100' : 'text-gray-400'}`}>
                              {fmt(slot.endTime)}
                            </span>
                            {isAvailable && (
                              <span className={`text-[10px] mt-1 font-semibold ${isSelected ? 'text-emerald-100' : 'text-emerald-600'}`}>
                                {slot.pricePerSlot.toLocaleString('vi-VN')}đ
                              </span>
                            )}
                          </button>
                        )
                      })}
                    </div>
                  )}
                </div>

                {/* Selected summary */}
                {state.selectedSlotId && (
                  <div className="bg-emerald-50 border border-emerald-100 rounded-xl p-4 flex items-start gap-3 animate-in fade-in duration-200">
                    <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
                    <div>
                      <p className="text-[13px] font-semibold text-emerald-800">Đã chọn</p>
                      <p className="text-[12px] text-emerald-700 mt-0.5">
                        {state.selectedCourtName} · {state.selectedSlotLabel}
                      </p>
                      <p className="text-[11px] text-emerald-600 mt-0.5">
                        {days.find(d => d.iso === state.selectedDate)?.dayName},{' '}
                        {days.find(d => d.iso === state.selectedDate)?.label}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* ── FOOTER ───────────────────────────────────────────────── */}
          {state.step === 2 && (
            <div className="border-t border-gray-100 px-5 py-4 shrink-0 bg-white">
              {!session && (
                <div className="flex items-center gap-2 text-[12px] text-amber-600 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2 mb-3">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>Bạn chưa đăng nhập — sẽ được yêu cầu đăng nhập khi xác nhận.</span>
                </div>
              )}
              <Button
                onClick={handleConfirm}
                disabled={!canConfirm}
                className="w-full rounded-xl py-6 font-bold bg-emerald-600 hover:bg-emerald-700 text-white shadow-lg shadow-emerald-200 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loadingSlots ? (
                  <Loader2 className="w-4 h-4 animate-spin mr-2" />
                ) : (
                  <CalendarDays className="w-4 h-4 mr-2" />
                )}
                Xác nhận đặt sân
              </Button>
            </div>
          )}
        </SheetContent>
      </Sheet>

      {/* ── LOGIN MODAL (outside Sheet to avoid z-index issues) ───── */}
      {showLoginModal && (
        <LoginModal
          onLogin={handleLogin}
          onCancel={() => setShowLoginModal(false)}
        />
      )}
    </>
  )
}
