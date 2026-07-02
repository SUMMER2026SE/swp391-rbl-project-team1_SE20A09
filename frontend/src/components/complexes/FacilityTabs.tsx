'use client'

import { IconAlertTriangle, IconTool, IconBallFootball } from '@tabler/icons-react'
import type { FacilityDto, StadiumStatus } from '@/types/complex'

interface FacilityTabsProps {
  facilities: FacilityDto[]
  selectedId: number | null
  onSelect: (id: number) => void
}

const STATUS_CONFIG: Record<StadiumStatus, { label: string; dotCls: string; disabled: boolean }> = {
  AVAILABLE: { label: 'Hoạt động', dotCls: 'bg-emerald-500', disabled: false },
  MAINTENANCE: { label: 'Bảo trì', dotCls: 'bg-amber-400', disabled: true },
  CLOSED: { label: 'Đóng cửa', dotCls: 'bg-red-500', disabled: true },
}

const SPORT_ICONS: Record<string, string> = {
  'Bóng đá': '⚽',
  'Cầu lông': '🏸',
  'Bóng rổ': '🏀',
  'Bóng chuyền': '🏐',
  'Tennis': '🎾',
  'Bơi lội': '🏊',
  'Pickle Ball': '🏓',
}

function formatTime(t: string) {
  return t?.substring(0, 5) ?? ''
}

export default function FacilityTabs({
  facilities,
  selectedId,
  onSelect,
}: FacilityTabsProps) {
  if (!facilities || facilities.length === 0) {
    return (
      <div className="text-center py-10 text-gray-400 text-sm">
        Cơ sở này chưa có khu sân nào.
      </div>
    )
  }

  return (
    <div className="w-full">
      <h2 className="text-lg font-semibold text-gray-800 mb-4">Chọn khu sân</h2>
      <div className="flex flex-wrap gap-3">
        {facilities.map((facility) => {
          // stadiumStatus riêng vẫn AVAILABLE khi bảo trì đến từ Complex cha (cascade) hoặc
          // MaintenanceSchedule theo khung ngày — dùng underMaintenanceToday để phản ánh đúng.
          const effectiveStatus: StadiumStatus =
            facility.stadiumStatus === 'AVAILABLE' && facility.underMaintenanceToday
              ? 'MAINTENANCE'
              : facility.stadiumStatus
          const statusCfg = STATUS_CONFIG[effectiveStatus] ?? STATUS_CONFIG.AVAILABLE
          const isSelected = selectedId === facility.stadiumId
          const sportIcon = facility.sportType
            ? (SPORT_ICONS[facility.sportType.sportName] ?? '🏟️')
            : '🏟️'

          return (
            <button
              key={facility.stadiumId}
              disabled={statusCfg.disabled}
              onClick={() => !statusCfg.disabled && onSelect(facility.stadiumId)}
              className={[
                'relative flex flex-col items-start gap-1 px-4 py-3 rounded-xl border text-left',
                'transition-all duration-200 min-w-[160px] max-w-[220px]',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500',
                isSelected
                  ? 'border-emerald-500 bg-emerald-50 shadow-sm shadow-emerald-100'
                  : statusCfg.disabled
                  ? 'border-gray-100 bg-gray-50 opacity-60 cursor-not-allowed'
                  : 'border-gray-200 bg-white hover:border-emerald-300 hover:bg-emerald-50/50 cursor-pointer',
              ].join(' ')}
              aria-pressed={isSelected}
              aria-label={`Chọn khu ${facility.stadiumName}`}
            >
              {/* Status dot */}
              <span
                className={`absolute top-2.5 right-2.5 w-2 h-2 rounded-full ${statusCfg.dotCls}`}
                title={statusCfg.label}
              />

              {/* Sport icon + name */}
              <span className="text-xl leading-none">{sportIcon}</span>
              <span className="text-sm font-semibold text-gray-800 leading-tight pr-4">
                {facility.stadiumName}
              </span>

              {/* Sport type */}
              {facility.sportType && (
                <span className="text-xs text-gray-500">
                  {facility.sportType.sportName}
                </span>
              )}

              {/* Operating hours */}
              {facility.openTime && facility.closeTime && (
                <span className="text-xs text-gray-400">
                  {formatTime(facility.openTime)} – {formatTime(facility.closeTime)}
                </span>
              )}

              {/* Maintenance/Closed warning */}
              {statusCfg.disabled && (
                <span className="flex items-center gap-1 text-[10px] text-amber-600 font-medium mt-0.5">
                  {effectiveStatus === 'MAINTENANCE' ? (
                    <><IconTool className="w-3 h-3" /> Đang bảo trì</>
                  ) : (
                    <><IconAlertTriangle className="w-3 h-3" /> Đã đóng cửa</>
                  )}
                </span>
              )}
            </button>
          )
        })}
      </div>
    </div>
  )
}
