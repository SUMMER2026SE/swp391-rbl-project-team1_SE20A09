'use client'

import { useState } from 'react'
import WeeklySchedule from '@/components/venues/WeeklySchedule'
import { Badge } from '@/components/ui/badge'
import {
  IconChevronDown,
  IconChevronUp,
  IconClock,
  IconCurrencyDong,
  IconAlertTriangle,
  IconTool,
} from '@tabler/icons-react'
import type { CourtWithSlotsDto } from '@/types/complex'

interface CourtScheduleAccordionProps {
  courts: CourtWithSlotsDto[]
  /** Pre-expand this courtId on mount (from URL ?courtId=...) */
  defaultOpenId?: number | null
  onSlotSelect: (slotId: number, date: string, courtId: number) => void
}

function formatPrice(n: number) {
  return n.toLocaleString('vi-VN') + 'đ/h'
}

const STATUS_CONFIG = {
  AVAILABLE: { label: 'Trống', cls: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
  MAINTENANCE: { label: 'Bảo trì', cls: 'bg-amber-100 text-amber-700 border-amber-200' },
  CLOSED: { label: 'Đóng', cls: 'bg-red-100 text-red-700 border-red-200' },
}

export default function CourtScheduleAccordion({
  courts,
  defaultOpenId,
  onSlotSelect,
}: CourtScheduleAccordionProps) {
  const [openIds, setOpenIds] = useState<Set<number>>(() => {
    const init = new Set<number>()
    if (defaultOpenId) init.add(defaultOpenId)
    else if (courts.length > 0) init.add(courts[0].stadiumId)
    return init
  })

  const toggle = (id: number) => {
    setOpenIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  if (!courts || courts.length === 0) {
    return (
      <div className="text-center py-10 text-gray-400 text-sm">
        Khu sân này chưa có sân lẻ nào.
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {courts.map((court) => {
        const isOpen = openIds.has(court.stadiumId)
        const statusCfg = STATUS_CONFIG[court.stadiumStatus] ?? STATUS_CONFIG.AVAILABLE
        const isDisabled = court.stadiumStatus !== 'AVAILABLE'

        return (
          <div
            key={court.stadiumId}
            className={[
              'rounded-xl border overflow-hidden transition-shadow',
              isOpen ? 'shadow-md border-emerald-200' : 'shadow-sm border-gray-100',
              isDisabled ? 'opacity-70' : '',
            ].join(' ')}
          >
            {/* Accordion header */}
            <button
              onClick={() => !isDisabled && toggle(court.stadiumId)}
              disabled={isDisabled}
              className={[
                'w-full flex items-center justify-between px-5 py-4 text-left',
                'transition-colors',
                isOpen ? 'bg-emerald-50' : 'bg-white hover:bg-gray-50',
                isDisabled ? 'cursor-not-allowed' : 'cursor-pointer',
              ].join(' ')}
              aria-expanded={isOpen}
              id={`court-header-${court.stadiumId}`}
            >
              <div className="flex items-center gap-3 min-w-0">
                <span className="text-lg">🏟️</span>
                <div className="min-w-0">
                  <p className="font-semibold text-gray-900 text-sm leading-tight">
                    {court.stadiumName}
                  </p>
                  <div className="flex items-center flex-wrap gap-2 mt-1">
                    <span className="flex items-center gap-1 text-xs text-gray-500">
                      <IconCurrencyDong className="w-3.5 h-3.5" />
                      {formatPrice(court.pricePerHour)}
                    </span>
                    <Badge
                      className={`text-[10px] px-1.5 py-0 border font-medium ${statusCfg.cls}`}
                    >
                      {statusCfg.label}
                    </Badge>
                    {isDisabled && (
                      <span className="flex items-center gap-0.5 text-[10px] text-amber-600">
                        {court.stadiumStatus === 'MAINTENANCE' ? (
                          <IconTool className="w-3 h-3" />
                        ) : (
                          <IconAlertTriangle className="w-3 h-3" />
                        )}
                        {court.stadiumStatus === 'MAINTENANCE' ? 'Đang bảo trì' : 'Đóng'}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {isOpen ? (
                <IconChevronUp className="w-5 h-5 text-emerald-600 flex-shrink-0" />
              ) : (
                <IconChevronDown className="w-5 h-5 text-gray-400 flex-shrink-0" />
              )}
            </button>

            {/* Accordion body — Weekly schedule grid */}
            {isOpen && !isDisabled && (
              <div
                className="px-4 pb-5 pt-3 bg-white border-t border-emerald-100"
                role="region"
                aria-labelledby={`court-header-${court.stadiumId}`}
              >
                <WeeklySchedule
                  stadiumId={court.stadiumId}
                  onSlotSelect={(slotId, date) =>
                    onSlotSelect(slotId, date, court.stadiumId)
                  }
                />
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
