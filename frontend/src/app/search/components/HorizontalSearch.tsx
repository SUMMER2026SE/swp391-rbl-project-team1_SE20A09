import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Navigation, Calendar, Clock, MapPin, Loader2, ChevronDown } from 'lucide-react'
import { ComplexSearchParams } from '@/types/complex'
import { SupportedLocationDto } from '@/lib/api/location'

interface HorizontalSearchProps {
  filters: ComplexSearchParams
  onFilterChange: <K extends keyof ComplexSearchParams>(key: K, value: ComplexSearchParams[K]) => void
  onGetLocation: () => void
  isLocating?: boolean
  locations: SupportedLocationDto[]
}

const ALL_SENTINEL = 'all'

export function HorizontalSearch({
  filters,
  onFilterChange,
  onGetLocation,
  isLocating = false,
  locations,
}: HorizontalSearchProps) {
  const selectedProvince = filters.province || ''
  const selectedDistrict = filters.district || ''

  const districtOptions =
    locations.find((l) => l.province === selectedProvince)?.districts ?? []

  const handleProvinceChange = (value: string) => {
    const province = value === ALL_SENTINEL ? '' : value
    onFilterChange('province', province)
    // Reset district khi đổi tỉnh
    onFilterChange('district', '')
  }

  const handleDistrictChange = (value: string) => {
    onFilterChange('district', value === ALL_SENTINEL ? '' : value)
  }

  return (
    <div className="w-[95%] max-w-5xl mx-auto relative z-10 -mt-16 mb-8">
      {/* --- Pill bar (giữ nguyên hoàn toàn) --- */}
      <div className="bg-white dark:bg-card p-4 rounded-full shadow-2xl border border-gray-100 dark:border-border flex flex-col md:flex-row items-center gap-4">

        {/* Keyword Search */}
        <div className="flex-1 flex items-center px-4 py-2 border-b md:border-b-0 md:border-r border-gray-200 dark:border-border w-full">
          <MapPin className="text-gray-400 mr-3 h-5 w-5 shrink-0" />
          <div className="flex-1">
            <label className="text-xs font-bold text-gray-800 dark:text-gray-300 uppercase tracking-wider block mb-1">Địa điểm / Tên sân</label>
            <Input
              placeholder="Bạn muốn tìm sân ở đâu?"
              value={filters.keyword || ''}
              onChange={(e) => onFilterChange('keyword', e.target.value)}
              className="border-0 shadow-none focus-visible:ring-0 p-0 text-sm placeholder:text-gray-400 h-auto bg-transparent"
            />
          </div>
        </div>

        {/* Date */}
        <div className="flex-1 flex items-center px-4 py-2 border-b md:border-b-0 md:border-r border-gray-200 dark:border-border w-full">
          <Calendar className="text-gray-400 mr-3 h-5 w-5 shrink-0" />
          <div className="flex-1">
            <label className="text-xs font-bold text-gray-800 dark:text-gray-300 uppercase tracking-wider block mb-1">Ngày đá</label>
            <Input
              type="date"
              value={filters.targetDate || ''}
              onChange={(e) => onFilterChange('targetDate', e.target.value)}
              className="border-0 shadow-none focus-visible:ring-0 p-0 text-sm h-auto bg-transparent"
            />
          </div>
        </div>

        {/* Time */}
        <div className="flex-1 flex items-center px-4 py-2 w-full">
          <Clock className="text-gray-400 mr-3 h-5 w-5 shrink-0" />
          <div className="flex-1 flex gap-2 items-center">
            <div className="flex-1">
              <label className="text-xs font-bold text-gray-800 dark:text-gray-300 uppercase tracking-wider block mb-1">Từ giờ</label>
              <Input
                type="time"
                value={filters.startTime || ''}
                onChange={(e) => onFilterChange('startTime', e.target.value)}
                className="border-0 shadow-none focus-visible:ring-0 p-0 text-sm h-auto bg-transparent"
              />
            </div>
            <span className="text-gray-300">-</span>
            <div className="flex-1">
              <label className="text-xs font-bold text-gray-800 dark:text-gray-300 uppercase tracking-wider block mb-1">Đến giờ</label>
              <Input
                type="time"
                value={filters.endTime || ''}
                onChange={(e) => onFilterChange('endTime', e.target.value)}
                className="border-0 shadow-none focus-visible:ring-0 p-0 text-sm h-auto bg-transparent"
              />
            </div>
          </div>
        </div>

        {/* Search Button (Location) */}
        <div className="md:pl-2 w-full md:w-auto">
          <Button
            onClick={onGetLocation}
            disabled={isLocating}
            className={`w-full md:w-auto rounded-full px-6 py-6 font-bold shadow-lg transition-all duration-300 active:scale-95
              ${isLocating
                ? 'bg-gray-400 cursor-not-allowed shadow-none'
                : filters.userLat
                  ? 'bg-blue-600 hover:bg-blue-700 shadow-blue-500/30 hover:scale-105'
                  : 'bg-primary hover:bg-primary/90 shadow-primary/30 hover:scale-105'
              }
            `}
          >
            {isLocating ? (
              <Loader2 className="mr-2 h-5 w-5 animate-spin" />
            ) : (
              <Navigation className={`mr-2 h-5 w-5 ${filters.userLat ? 'animate-pulse' : ''}`} />
            )}
            {isLocating ? 'Đang định vị...' : filters.userLat ? 'Gần bạn (15km)' : 'Gần tôi'}
          </Button>
        </div>
      </div>

      {/* --- Hàng dropdown Tỉnh/Thành + Quận/Huyện (mới) --- */}
      {locations.length > 0 && (
        <div className="mt-3 bg-white dark:bg-card rounded-2xl shadow-md border border-gray-100 dark:border-border px-4 py-3 flex flex-col sm:flex-row gap-3 items-center">
          <ChevronDown className="hidden sm:block text-gray-400 h-4 w-4 shrink-0" />

          {/* Select Tỉnh/Thành */}
          <div className="flex-1 w-full sm:w-auto">
            <label className="text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider block mb-1.5">
              Tỉnh / Thành phố
            </label>
            <Select
              value={selectedProvince || ALL_SENTINEL}
              onValueChange={handleProvinceChange}
            >
              <SelectTrigger className="w-full border-gray-200 dark:border-border rounded-xl text-sm focus:ring-primary/30">
                <SelectValue placeholder="Tất cả tỉnh/thành" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL_SENTINEL}>Tất cả tỉnh/thành</SelectItem>
                {locations.map((l) => (
                  <SelectItem key={l.province} value={l.province}>
                    {l.province}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Divider */}
          <div className="hidden sm:block h-8 w-px bg-gray-200 dark:bg-border" />

          {/* Select Quận/Huyện */}
          <div className="flex-1 w-full sm:w-auto">
            <label className="text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider block mb-1.5">
              Quận / Huyện
            </label>
            <Select
              value={selectedDistrict || ALL_SENTINEL}
              onValueChange={handleDistrictChange}
              disabled={!selectedProvince}
            >
              <SelectTrigger
                className={`w-full border-gray-200 dark:border-border rounded-xl text-sm focus:ring-primary/30 ${
                  !selectedProvince ? 'opacity-50 cursor-not-allowed' : ''
                }`}
              >
                <SelectValue placeholder={selectedProvince ? 'Tất cả quận/huyện' : 'Chọn tỉnh/thành trước'} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL_SENTINEL}>Tất cả quận/huyện</SelectItem>
                {districtOptions.map((d) => (
                  <SelectItem key={d} value={d}>
                    {d}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
      )}
    </div>
  )
}
