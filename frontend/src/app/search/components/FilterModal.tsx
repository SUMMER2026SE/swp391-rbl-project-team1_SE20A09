import { Button } from '@/components/ui/button'
import { Slider } from '@/components/ui/slider'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import { Filter, X } from 'lucide-react'
import { StadiumSearchRequest, Amenity } from '@/lib/api/stadium'
import { useState } from 'react'

interface FilterModalProps {
  filters: StadiumSearchRequest
  amenitiesList: Amenity[]
  totalResults: number
  onFilterChange: <K extends keyof StadiumSearchRequest>(key: K, value: StadiumSearchRequest[K]) => void
  onAmenityToggle: (id: number) => void
  onClearFilters: () => void
}

export function FilterModal({ filters, amenitiesList, totalResults, onFilterChange, onAmenityToggle, onClearFilters }: FilterModalProps) {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <Sheet open={isOpen} onOpenChange={setIsOpen}>
      <SheetTrigger asChild>
        <Button variant="outline" className="rounded-full shadow-sm border-gray-200 dark:border-border font-semibold hover:bg-gray-50 dark:hover:bg-muted">
          <Filter className="mr-2 h-4 w-4" /> 
          Bộ lọc nâng cao
          {filters.amenityIds && filters.amenityIds.length > 0 && (
            <span className="ml-2 bg-primary text-white w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold">
              {filters.amenityIds.length}
            </span>
          )}
        </Button>
      </SheetTrigger>
      
      <SheetContent side="right" className="w-full sm:max-w-md p-0 flex flex-col h-full bg-background">
        <SheetHeader className="p-6 border-b border-gray-100 dark:border-border flex flex-row items-center justify-between sticky top-0 bg-background z-10">
          <SheetTitle className="text-xl font-bold">Bộ lọc nâng cao</SheetTitle>
          <Button variant="ghost" size="sm" onClick={onClearFilters} className="text-muted-foreground hover:text-destructive p-0 h-auto font-medium">
            Xóa tất cả
          </Button>
        </SheetHeader>
        
        <div className="p-6 overflow-y-auto flex-1 space-y-8">
          
          {/* Price Range */}
          <div>
            <h4 className="font-bold text-lg mb-4 flex justify-between items-end">
              <span>Khoảng giá</span>
              <span className="text-primary text-sm">
                {(filters.minPrice ?? 0) / 1000}k - {(filters.maxPrice ?? 1000000) / 1000}k
              </span>
            </h4>
            <div className="px-2 pt-4 pb-2">
              <Slider
                min={0}
                max={1000000}
                step={50000}
                value={[filters.minPrice || 0, filters.maxPrice || 1000000]}
                onValueChange={(val) => {
                  onFilterChange('minPrice', val[0])
                  onFilterChange('maxPrice', val[1])
                }}
              />
            </div>
          </div>

          {/* Distance Radius */}
          {filters.userLat && (
            <div>
              <h4 className="font-bold text-lg mb-4 flex justify-between items-end">
                <span>Bán kính tìm kiếm</span>
                <span className="text-primary text-sm">
                  {filters.radiusInKm || 15} km
                </span>
              </h4>
              <div className="px-2 pt-4 pb-2">
                <Slider
                  min={1}
                  max={50}
                  step={1}
                  value={[filters.radiusInKm || 15]}
                  onValueChange={(val) => {
                    onFilterChange('radiusInKm', val[0])
                  }}
                />
              </div>
            </div>
          )}

          {/* Amenities */}
          <div>
            <h4 className="font-bold text-lg mb-4">Tiện ích (Amenities)</h4>
            <div className="flex flex-wrap gap-2.5">
              {amenitiesList.map(amenity => {
                const isSelected = filters.amenityIds?.includes(amenity.amenityId)
                return (
                  <button
                    key={amenity.amenityId}
                    onClick={() => onAmenityToggle(amenity.amenityId)}
                    className={`px-4 py-2.5 text-sm font-semibold rounded-full transition-all duration-200 border
                      ${isSelected 
                        ? 'bg-primary text-primary-foreground border-primary shadow-md shadow-primary/20 scale-105' 
                        : 'bg-white dark:bg-card text-gray-700 dark:text-gray-300 border-gray-200 dark:border-border hover:border-primary/50'
                      }
                    `}
                  >
                    {amenity.name}
                  </button>
                )
              })}
            </div>
          </div>
          
        </div>
        
        {/* Footer */}
        <div className="p-4 border-t border-gray-100 dark:border-border bg-background">
          <Button 
            className="w-full rounded-xl py-6 text-lg font-bold shadow-lg" 
            onClick={() => setIsOpen(false)}
          >
            Hiển thị {totalResults} sân
          </Button>
        </div>
      </SheetContent>
    </Sheet>
  )
}
