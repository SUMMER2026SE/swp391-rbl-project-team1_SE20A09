import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Navigation, Loader2, Search } from 'lucide-react'
import { ComplexSearchParams } from '@/types/complex'

interface HorizontalSearchProps {
  filters: ComplexSearchParams
  onFilterChange: <K extends keyof ComplexSearchParams>(key: K, value: ComplexSearchParams[K]) => void
  onGetLocation: () => void
  isLocating?: boolean
}

export function HorizontalSearch({ filters, onFilterChange, onGetLocation, isLocating = false }: HorizontalSearchProps) {
  return (
    <div className="bg-white dark:bg-card p-3 md:p-4 rounded-full shadow-[0_8px_30px_rgb(0,0,0,0.08)] border border-gray-100 dark:border-border max-w-4xl mx-auto flex flex-col md:flex-row items-center gap-3 relative z-10 -mt-10 mb-8 w-[95%]">
      
      {/* 1. Keyword Search (Chiếm phần lớn diện tích, thoáng đãng) */}
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
            className="border-0 shadow-none focus-visible:ring-0 p-0 text-base font-medium text-gray-900 dark:text-white placeholder:text-gray-300 h-auto bg-transparent w-full"
          />
        </div>
      </div>

      {/* Khoảng phân cách mềm (Divider) */}
      <div className="hidden md:block w-[1px] h-10 bg-gray-200 dark:bg-border mx-2"></div>

      {/* 2. Action Buttons Area */}
      <div className="flex items-center gap-3 w-full md:w-auto px-4 md:px-0 pb-2 md:pb-0">
        
        {/* Nút Tìm quanh đây (Định vị) */}
        <Button
          type="button"
          variant="outline"
          onClick={onGetLocation}
          disabled={isLocating}
          title="Tìm sân gần tôi"
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
          className="w-full md:w-auto rounded-full px-8 h-14 font-bold shadow-lg shadow-primary/20 bg-primary hover:bg-primary/90 hover:scale-105 active:scale-95 transition-all duration-300 text-white text-base tracking-wide"
        >
          Tìm Kiếm
        </Button>
      </div>
    </div>
  )
}
