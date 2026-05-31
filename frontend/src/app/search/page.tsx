'use client'

import { useState, useEffect, useCallback } from 'react'
import { searchStadiums, getAmenities, getSportTypes, StadiumResponse, StadiumSearchRequest, Amenity } from '@/lib/api/stadium'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Card, CardContent, CardFooter } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { MapPin, Navigation, Filter } from 'lucide-react'

export default function SearchPage() {
  const [stadiums, setStadiums] = useState<StadiumResponse[]>([])
  const [amenitiesList, setAmenitiesList] = useState<Amenity[]>([])
  const [sportTypes, setSportTypes] = useState<{ sportTypeId: number, sportName: string }[]>([])
  const [loading, setLoading] = useState(false)

  const [filters, setFilters] = useState<StadiumSearchRequest>({
    keyword: '',
    sportTypeId: undefined,
    targetDate: '',
    startTime: '',
    endTime: '',
    amenityIds: [],
    userLat: undefined,
    userLng: undefined,
    radiusInKm: undefined,
    page: 0,
    size: 10,
  })

  useEffect(() => {
    Promise.all([getAmenities(), getSportTypes()])
      .then(([amenitiesRes, sportTypesRes]) => {
        setAmenitiesList(amenitiesRes)
        setSportTypes(sportTypesRes)
      })
      .catch(console.error)
  }, [])

  const fetchStadiums = useCallback(async () => {
    setLoading(true)
    try {
      const res = await searchStadiums(filters)
      setStadiums(res.content)
    } catch (error) {
      console.error(error)
    } finally {
      setLoading(false)
    }
  }, [filters])

  useEffect(() => {
    fetchStadiums()
  }, [fetchStadiums])

  const handleFilterChange = (key: keyof StadiumSearchRequest, value: any) => {
    setFilters(prev => ({ ...prev, [key]: value, page: 0 }))
  }

  const handleAmenityChange = (id: number, checked: boolean) => {
    setFilters(prev => {
      const ids = prev.amenityIds || []
      return {
        ...prev,
        amenityIds: checked ? [...ids, id] : ids.filter(i => i !== id),
        page: 0
      }
    })
  }

  const getLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          handleFilterChange('userLat', position.coords.latitude)
          handleFilterChange('userLng', position.coords.longitude)
          handleFilterChange('radiusInKm', 10) // default 10km radius
        },
        (error) => alert("Không thể lấy vị trí của bạn")
      )
    } else {
      alert("Trình duyệt của bạn không hỗ trợ định vị")
    }
  }

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="flex flex-col md:flex-row gap-8">
        
        {/* Sidebar Filters */}
        <div className="w-full md:w-80 shrink-0 space-y-6 bg-card p-6 rounded-xl border shadow-sm h-fit">
          <h3 className="text-xl font-bold flex items-center mb-6 text-primary"><Filter className="mr-2 h-5 w-5"/> Bộ Lọc Tìm Kiếm</h3>
          
          <div className="space-y-4">
            <div>
              <Input 
                placeholder="Tìm tên sân, khu vực..." 
                value={filters.keyword || ''}
                onChange={(e) => handleFilterChange('keyword', e.target.value)}
                className="bg-background"
              />
            </div>

            <div>
              <h4 className="font-semibold text-sm mb-2 text-foreground/80">Môn thể thao</h4>
              <Select onValueChange={(v) => handleFilterChange('sportTypeId', v === 'all' ? undefined : Number(v))}>
                <SelectTrigger className="bg-background">
                  <SelectValue placeholder="Tất cả môn" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả môn</SelectItem>
                  {sportTypes.map(st => (
                    <SelectItem key={st.sportTypeId} value={st.sportTypeId.toString()}>{st.sportName}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <h4 className="font-semibold text-sm mb-2 text-foreground/80">Gần bạn</h4>
              <Button variant={filters.userLat ? "default" : "outline"} className="w-full justify-start" onClick={getLocation}>
                <Navigation className="mr-2 h-4 w-4" /> 
                {filters.userLat ? 'Đã lấy vị trí (Bán kính 10km)' : 'Sử dụng vị trí của tôi'}
              </Button>
            </div>

            <div className="pt-4 border-t">
              <h4 className="font-semibold text-sm mb-3 text-foreground/80">Khung giờ trống</h4>
              <div className="space-y-3">
                <Input type="date" value={filters.targetDate || ''} onChange={(e) => handleFilterChange('targetDate', e.target.value)} className="bg-background" />
                <div className="flex gap-2">
                  <Input type="time" placeholder="Từ" value={filters.startTime || ''} onChange={(e) => handleFilterChange('startTime', e.target.value)} className="bg-background" />
                  <Input type="time" placeholder="Đến" value={filters.endTime || ''} onChange={(e) => handleFilterChange('endTime', e.target.value)} className="bg-background" />
                </div>
              </div>
            </div>

            <div className="pt-4 border-t">
              <h4 className="font-semibold text-sm mb-3 text-foreground/80">Tiện ích bắt buộc</h4>
              <div className="space-y-3 max-h-48 overflow-y-auto pr-2">
                {amenitiesList.map(amenity => (
                  <div key={amenity.amenityId} className="flex items-center space-x-3">
                    <Checkbox 
                      id={`amenity-${amenity.amenityId}`} 
                      checked={filters.amenityIds?.includes(amenity.amenityId) || false}
                      onCheckedChange={(checked) => handleAmenityChange(amenity.amenityId, checked as boolean)}
                    />
                    <label htmlFor={`amenity-${amenity.amenityId}`} className="text-sm cursor-pointer hover:text-primary transition-colors">{amenity.name}</label>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Results */}
        <div className="flex-1">
          <div className="mb-6 flex justify-between items-end">
            <h2 className="text-2xl font-bold tracking-tight">Kết quả tìm kiếm</h2>
            <span className="text-muted-foreground font-medium bg-muted px-3 py-1 rounded-full text-sm">{stadiums.length} sân phù hợp</span>
          </div>

          {loading ? (
            <div className="flex justify-center items-center py-32">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
            </div>
          ) : stadiums.length === 0 ? (
            <div className="text-center py-32 bg-card rounded-xl border border-dashed">
              <div className="text-muted-foreground mb-2">Không tìm thấy sân thể thao nào phù hợp với bộ lọc.</div>
              <Button variant="link" onClick={() => setFilters({ keyword: '', page: 0, size: 10, amenityIds: [] })}>Xóa bộ lọc</Button>
            </div>
          ) : (
            <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
              {stadiums.map((stadium) => (
                <Card key={stadium.stadiumId} className="overflow-hidden hover:shadow-xl transition-all duration-300 border-border group cursor-pointer">
                  <div className="relative h-56 w-full bg-muted overflow-hidden">
                    {stadium.firstImageUrl ? (
                      <img src={stadium.firstImageUrl} alt={stadium.stadiumName} className="object-cover w-full h-full group-hover:scale-105 transition-transform duration-500" />
                    ) : (
                      <div className="flex items-center justify-center h-full text-muted-foreground bg-secondary/50">Không có ảnh</div>
                    )}
                    <div className="absolute top-3 left-3 bg-background/90 backdrop-blur-sm text-foreground px-3 py-1 rounded-full text-sm font-semibold shadow-sm border">
                      {stadium.sportTypeName}
                    </div>
                  </div>
                  <CardContent className="p-5">
                    <h3 className="text-xl font-bold truncate mb-2 group-hover:text-primary transition-colors">{stadium.stadiumName}</h3>
                    <p className="text-sm text-muted-foreground flex items-start gap-1.5 mb-3">
                      <MapPin className="h-4 w-4 shrink-0 mt-0.5 text-primary/70" /> 
                      <span className="line-clamp-2">{stadium.address}</span>
                    </p>
                    
                    {stadium.distanceInKm && (
                      <div className="mb-3 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 border border-blue-200">
                        <Navigation className="h-3 w-3 mr-1" />
                        Cách bạn {stadium.distanceInKm.toFixed(1)} km
                      </div>
                    )}
                    
                    <div className="flex flex-wrap gap-1.5 mt-4">
                      {stadium.amenities.slice(0, 4).map(a => (
                        <span key={a.amenityId} className="text-[11px] bg-secondary/80 text-secondary-foreground px-2 py-1 rounded-md font-medium border border-border/50">{a.name}</span>
                      ))}
                      {stadium.amenities.length > 4 && (
                        <span className="text-[11px] bg-secondary/80 px-2 py-1 rounded-md font-medium">+{stadium.amenities.length - 4}</span>
                      )}
                    </div>
                  </CardContent>
                  <CardFooter className="p-5 border-t flex justify-between items-center bg-muted/20">
                    <div>
                      <div className="font-bold text-xl text-primary">{stadium.pricePerHour.toLocaleString('vi-VN')}đ</div>
                      <div className="text-xs text-muted-foreground">mỗi giờ</div>
                    </div>
                    <Button className="rounded-full px-6 font-semibold shadow-md">Đặt sân ngay</Button>
                  </CardFooter>
                </Card>
              ))}
            </div>
          )}
        </div>
        
      </div>
    </div>
  )
}
