import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Navigation, Loader2, Search, ChevronDown } from 'lucide-react'
import { ComplexSearchParams } from '@/types/complex'
import { SupportedLocationDto } from '@/lib/api/location'

interface HorizontalSearchProps {
  filters: ComplexSearchParams
  onFilterChange: <K extends keyof ComplexSearchParams>(key: K, value: ComplexSearchParams[K]) => void
  onGetLocation: () => void
  onSearch: () => void
  isLocating?: boolean
  locations: SupportedLocationDto[]
}

const ALL_SENTINEL = 'all'

export function HorizontalSearch({
  filters,
  onFilterChange,
  onGetLocation,
  onSearch,
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
    <div className="w-[95%] max-w-4xl mx-auto relative z-10 -mt-10 mb-8">
      {/* 1. Thanh tìm kiếm chính kiểu Airbnb */}
      <div className="bg-white dark:bg-card p-3 md:p-4 rounded-full shadow-[0_8px_30px_rgb(0,0,0,0.08)] border border-gray-100 dark:border-border flex flex-col md:flex-row items-center gap-3 w-full">
        {/* Keyword Search (Chiếm phần lớn diện tích, thoáng đãng) */}
        <div className="flex-1 flex items-center px-6 py-2 w-full group">
          <Search className="text-gray-400 group-focus-within:text-primary transition-colors mr-4 h-6 w-6 shrink-0" />
          <div className="flex-1">
            <label className="text-[11px] font-extrabold text-gray-800 dark:text-gray-300 uppercase tracking-widest block mb-0.5">
              Địa điểm / Tên sân
            </label>
            <Input
              placeholder="Tìm kiếm khu vực, tên tổ hợp sân..."
              value={filters.keyword || ''}
              onChange={(e) => onFilterChange('keyword', e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') onSearch()
              }}
              className="border-0 shadow-none focus-visible:ring-0 p-0 text-base font-medium text-gray-900 dark:text-white placeholder:text-gray-300 h-auto bg-transparent w-full"
            />
          </div>
        </div>

        {/* Khoảng phân cách mềm (Divider) */}
        <div className="hidden md:block w-[1px] h-10 bg-gray-200 dark:bg-border mx-2"></div>

        {/* Action Buttons Area */}
        <div className="flex items-center gap-3 w-full md:w-auto px-4 md:px-0 pb-2 md:pb-0">
          {/* Nút Tìm quanh đây (Định vị) */}
          <Button
            type="button"
            variant="outline"
            onClick={onGetLocation}
            disabled={isLocating}
            title="Tìm sân gần tôi"
            aria-label="Tìm sân gần tôi"
            className={`rounded-full h-14 w-14 p-0 flex items-center justify-center font-semibold shadow-sm border-gray-200 transition-all duration-300
              ${isLocating
                ? 'bg-gray-100 cursor-not-allowed'
                : filters.userLat
                  ? 'bg-blue-50 text-blue-600 border-blue-200 shadow-inner'
                  : 'hover:bg-gray-50 text-gray-600'
              }
            `}
          >
            {isLocating ? (
              <Loader2 className="h-5 w-5 animate-spin text-gray-400" />
            ) : (
              <Navigation className={`h-5 w-5 ${filters.userLat ? 'animate-pulse' : ''}`} />
            )}
          </Button>

          {/* Nút Tìm kiếm chính */}
          <Button
            type="button"
            onClick={onSearch}
            className="w-full md:w-auto rounded-full px-8 h-14 font-bold shadow-lg shadow-primary/20 bg-primary hover:bg-primary/90 hover:scale-105 active:scale-95 transition-all duration-300 text-white text-base tracking-wide"
          >
            Tìm Kiếm
          </Button>
        </div>
      </div>

      {/* 2. Hàng dropdown Tỉnh/Thành + Quận/Huyện (mới) */}
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
