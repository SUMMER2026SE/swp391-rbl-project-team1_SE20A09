'use client'

import { useState, useEffect, useRef } from 'react'
import dynamic from 'next/dynamic'
import Image from 'next/image'
import {
  IconBallFootball,
  IconClock,
  IconClockOff,
  IconCategory,
  IconCircleCheck,
  IconCircleX,
  IconMapPin,
  IconMap2,
  IconStar,
  IconStarHalf,
  IconCalendar,
  IconCalendarPlus,
  IconPackage,
  IconMessageCircle,
  IconMessageOff,
  IconPhone,
  IconCar,
  IconUser,
  IconDroplet,
  IconCoffee,
  IconWifi,
  IconInfoCircle,
  IconMaximize,
  IconX,
  IconArrowLeft,
  IconArrowRight,
  IconChevronLeft,
  IconChevronRight
} from '@tabler/icons-react'

// Lazy load the Leaflet Map to avoid SSR issues and reduce bundle size
const VenueMap = dynamic(() => import('./VenueMap'), {
  ssr: false,
  loading: () => (
    <div className="h-[180px] bg-gray-100 flex items-center justify-center rounded-[8px] animate-pulse border-[0.5px] border-gray-200">
      <span className="text-[12px] text-gray-400">Đang tải bản đồ...</span>
    </div>
  )
})

export interface VenueDetailProps {
  venue: {
    id: number
    name: string
    sport: string
    address: string
    rating: number
    reviewCount: number
    pricePerHour: number
    openTime: string        // "06:00"
    closeTime: string       // "22:00"
    status: string
    description: string
    images: string[]        // array of image URLs, length >= 5
    amenities: string[]
    timeSlots: {
      date: string          // "YYYY-MM-DD"
      hour: string          // "07:00"
      available: boolean
    }[]
    services: {
      name: string
      stock: number
      price: number
    }[]
    owner: {
      name: string
      initials: string
      phone: string
    }
    coordinates: {
      lat: number
      lng: number
    }
    recentReviews?: {
      reviewId: number
      userName: string
      userAvatar: string | null
      ratingScore: number
      comment: string
      ownerResponse: string | null
      createdAt: string
    }[]
  }
}

const IMAGE_LABELS = [
  "Sân chính — góc nhìn tổng thể",
  "Cỏ nhân tạo thế hệ 3",
  "Phòng thay đồ",
  "Bãi đỗ xe",
  "Căng tin & khu vực nghỉ"
]

export default function VenueDetail({ venue }: VenueDetailProps) {
  const [activeIndex, setActiveIndex] = useState(0)
  const [activeTab, setActiveTab] = useState<string>('overview')
  const [lightboxOpen, setLightboxOpen] = useState(false)
  const [lightboxIndex, setLightboxIndex] = useState(0)
  const [mapLoaded, setMapLoaded] = useState(false)

  // Date picker state
  const [selectedDate, setSelectedDate] = useState<Date>(() => {
    const d = new Date()
    d.setHours(0, 0, 0, 0)
    return d
  })
  const [popoverOpen, setPopoverOpen] = useState(false)
  const [viewMonth, setViewMonth] = useState<number>(selectedDate.getMonth())
  const [viewYear, setViewYear] = useState<number>(selectedDate.getFullYear())
  const [selectedSlot, setSelectedSlot] = useState<string | null>(null)

  const datePickerRef = useRef<HTMLDivElement>(null)

  // Lazy load map when map tab becomes active
  useEffect(() => {
    if (activeTab === 'location') {
      setMapLoaded(true)
    }
  }, [activeTab])

  // Reset selected slot when date changes
  useEffect(() => {
    setSelectedSlot(null)
  }, [selectedDate])

  // Click outside listener for date picker popover
  useEffect(() => {
    if (!popoverOpen) return
    const handleClickOutside = (e: MouseEvent) => {
      if (datePickerRef.current && !datePickerRef.current.contains(e.target as Node)) {
        setPopoverOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [popoverOpen])

  // Lightbox keyboard navigation
  useEffect(() => {
    if (!lightboxOpen) return

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft') {
        setLightboxIndex(prev => (prev > 0 ? prev - 1 : venue.images.length - 1))
      } else if (e.key === 'ArrowRight') {
        setLightboxIndex(prev => (prev < venue.images.length - 1 ? prev + 1 : 0))
      } else if (e.key === 'Escape') {
        setLightboxOpen(false)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [lightboxOpen, venue.images.length])

  const openLightbox = (index: number) => {
    setLightboxIndex(index)
    setLightboxOpen(true)
  }

  // Get active date formatted as YYYY-MM-DD
  const getSelectedDateString = () => {
    const year = selectedDate.getFullYear()
    const month = String(selectedDate.getMonth() + 1).padStart(2, '0')
    const day = String(selectedDate.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  // Filter & construct slots for the selected date
  const getSlotsForSelectedDate = () => {
    const dateStr = getSelectedDateString()
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const isToday = selectedDate.getTime() === today.getTime()
    const currentHour = new Date().getHours()
    const currentMinutes = new Date().getMinutes()

    // 16 slots from 06:00 to 22:00
    return Array.from({ length: 16 }, (_, i) => {
      const h = i + 6
      const hourStr = `${String(h).padStart(2, '0')}:00`
      const timeLabel = `${hourStr} – ${String(h + 1).padStart(2, '0')}:00`

      let isPast = false
      if (isToday) {
        if (currentHour > h) {
          isPast = true
        } else if (currentHour === h && currentMinutes > 0) {
          isPast = true
        }
      }

      // Check slot status
      const matched = venue.timeSlots.find(s => s.date === dateStr && s.hour === hourStr)
      const available = matched ? matched.available : true

      return {
        hour: h,
        hourStr,
        timeLabel,
        available,
        isPast
      }
    })
  }

  const dateSlots = getSlotsForSelectedDate()
  const availableSlotsCount = dateSlots.filter(s => s.available && !s.isPast).length

  // Date picker date check helpers
  const todayDateObj = new Date()
  todayDateObj.setHours(0, 0, 0, 0)

  const isPastDay = (date: Date) => {
    const check = new Date(date)
    check.setHours(0, 0, 0, 0)
    return check.getTime() < todayDateObj.getTime()
  }

  const isTodayDay = (date: Date) => {
    const check = new Date(date)
    check.setHours(0, 0, 0, 0)
    return check.getTime() === todayDateObj.getTime()
  }

  const isSelectedDay = (date: Date) => {
    const check = new Date(date)
    check.setHours(0, 0, 0, 0)
    const sel = new Date(selectedDate)
    sel.setHours(0, 0, 0, 0)
    return check.getTime() === sel.getTime()
  }

  const formatDatePickerLabel = (date: Date) => {
    const days = ['Chủ Nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7']
    const dayName = days[date.getDay()]
    const dd = String(date.getDate()).padStart(2, '0')
    const mm = String(date.getMonth() + 1).padStart(2, '0')
    const yyyy = date.getFullYear()
    return `${dayName}, ${dd}/${mm}/${yyyy}`
  }

  const getMonthYearLabel = (month: number, year: number) => {
    const monthStr = String(month + 1).padStart(2, '0')
    return `Tháng ${monthStr}, ${year}`
  }

  const handlePrevMonth = () => {
    if (viewYear > todayDateObj.getFullYear() || (viewYear === todayDateObj.getFullYear() && viewMonth > todayDateObj.getMonth())) {
      if (viewMonth === 0) {
        setViewMonth(11)
        setViewYear(prev => prev - 1)
      } else {
        setViewMonth(prev => prev - 1)
      }
    }
  }

  const handleNextMonth = () => {
    if (viewMonth === 11) {
      setViewMonth(0)
      setViewYear(prev => prev + 1)
    } else {
      setViewMonth(prev => prev + 1)
    }
  }

  const handleSelectToday = () => {
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    setSelectedDate(today)
    setViewMonth(today.getMonth())
    setViewYear(today.getFullYear())
    setPopoverOpen(false)
  }

  const generateCalendarDays = (year: number, month: number) => {
    const startOfMonth = new Date(year, month, 1)
    const startDayOfWeek = startOfMonth.getDay() // 0 = Sunday
    const startDate = new Date(year, month, 1 - startDayOfWeek)
    const days: Date[] = []
    for (let i = 0; i < 42; i++) {
      const d = new Date(startDate)
      d.setDate(startDate.getDate() + i)
      days.push(d)
    }
    return days
  }

  // Back button helper
  const handleBack = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (typeof window !== 'undefined') {
      if (window.history.length > 1) {
        window.history.back()
      } else {
        window.location.href = '/search'
      }
    }
  }

  // Booking CTA helper
  const handleBookingRedirect = () => {
    const dateStr = getSelectedDateString()
    let url = `/booking/new?venueId=${venue.id}&date=${dateStr}`
    if (selectedSlot) {
      url += `&slot=${selectedSlot}`
    }
    window.location.href = url
  }

  // Star renderer helper
  const renderStars = (rating: number, size = 15) => {
    const stars = []
    const fullStars = Math.floor(rating)
    const hasHalf = rating % 1 !== 0
    for (let i = 0; i < 5; i++) {
      if (i < fullStars) {
        stars.push(<IconStar key={i} size={size} className="fill-[#f0a500] text-[#f0a500]" />)
      } else if (i === fullStars && hasHalf) {
        stars.push(<IconStarHalf key={i} size={size} className="fill-[#f0a500] text-[#f0a500]" />)
      } else {
        stars.push(<IconStar key={i} size={size} className="text-gray-300" />)
      }
    }
    return stars
  }

  const getAmenityIcon = (name: string) => {
    switch (name.toLowerCase()) {
      case 'bãi đỗ xe':
      case 'parking':
        return <IconCar className="w-[13px] h-[13px] text-[#1a8a4a] mr-[6px] shrink-0" />
      case 'phòng thay đồ':
      case 'changing room':
        return <IconUser className="w-[13px] h-[13px] text-[#1a8a4a] mr-[6px] shrink-0" />
      case 'nước miễn phí':
      case 'free water':
        return <IconDroplet className="w-[13px] h-[13px] text-[#1a8a4a] mr-[6px] shrink-0" />
      case 'căng tin':
      case 'canteen':
        return <IconCoffee className="w-[13px] h-[13px] text-[#1a8a4a] mr-[6px] shrink-0" />
      case 'wifi':
        return <IconWifi className="w-[13px] h-[13px] text-[#1a8a4a] mr-[6px] shrink-0" />
      default:
        return <IconCircleCheck className="w-[13px] h-[13px] text-[#1a8a4a] mr-[6px] shrink-0" />
    }
  }

  const isCurrentMonth = viewYear === todayDateObj.getFullYear() && viewMonth === todayDateObj.getMonth()

  // Reusable Contact Card
  const ContactCard = () => (
    <div className="bg-gray-50 border-[0.5px] border-gray-200 rounded-[8px] p-3 flex flex-col gap-3">
      <div className="flex items-center gap-3">
        {/* Initials avatar */}
        <div className="w-9 h-9 rounded-full bg-[#d4f0e2] text-[#1a8a4a] font-medium text-[12px] flex items-center justify-center uppercase shrink-0">
          {venue.owner.initials || 'HH'}
        </div>
        <div className="leading-tight min-w-0">
          <span className="block text-[13px] font-medium text-gray-750 truncate">{venue.owner.name}</span>
          <div className="flex items-center gap-1 mt-0.5 text-gray-400">
            <IconPhone className="w-3.5 h-3.5 text-[#1a8a4a] shrink-0" />
            <span className="text-[12px] font-normal">{venue.owner.phone}</span>
          </div>
        </div>
      </div>
      <div className="flex gap-2">
        <a 
          href={`tel:${venue.owner.phone}`}
          className="flex-1 flex items-center justify-center gap-1.5 border-[0.5px] border-gray-200 rounded-[20px] py-[5px] text-[12px] font-medium text-gray-600 hover:bg-gray-100 cursor-pointer transition-colors"
        >
          <IconPhone className="w-[13px] h-[13px] text-[#1a8a4a]" />
          <span>Gọi ngay</span>
        </a>
        <button 
          onClick={() => alert(`Nhắn tin đến ${venue.owner.phone}`)}
          className="flex-1 flex items-center justify-center gap-1.5 border-[0.5px] border-gray-200 rounded-[20px] py-[5px] text-[12px] font-medium text-gray-600 hover:bg-gray-100 cursor-pointer transition-colors"
        >
          <IconMessageCircle className="w-[13px] h-[13px] text-[#1a8a4a]" />
          <span>Nhắn tin</span>
        </button>
      </div>
    </div>
  )

  return (
    <div className="w-full min-h-screen bg-gray-50/50 select-none relative flex flex-col font-sans pb-12">
      <div className="w-full max-w-5xl mx-auto bg-white shadow-sm md:rounded-2xl md:my-6 overflow-hidden border border-gray-200">
        
        {/* [A] HERO IMAGE SECTION — full width within container */}
        <div 
          onClick={() => openLightbox(0)}
          className="w-full h-[320px] relative overflow-hidden cursor-pointer bg-[#1e4535]"
        >
        {venue.images[activeIndex] ? (
          <Image 
            src={venue.images[activeIndex]} 
            alt={venue.name} 
            fill
            className="object-cover transition-all duration-300"
            unoptimized
          />
        ) : (
          <div className="w-full h-full bg-[#1e4535] flex items-center justify-center">
            <span className="text-white/40 text-[14px]">Hình ảnh sân</span>
          </div>
        )}
        
        {/* Dark overlay */}
        <div className="absolute inset-0 bg-black/32" />

        {/* Circular back button */}
        <button 
          onClick={handleBack}
          className="absolute top-3 left-3 bg-black/40 hover:bg-black/60 text-white w-[34px] h-[34px] rounded-full flex items-center justify-center border-[0.5px] border-white/10 transition-all z-10"
        >
          <IconChevronLeft className="w-5 h-5" />
        </button>

        {/* Maximize Pill Button */}
        <button 
          onClick={(e) => {
            e.stopPropagation()
            openLightbox(activeIndex)
          }}
          className="absolute top-3 right-3 bg-white/20 hover:bg-white/30 text-white px-3 py-1.5 rounded-[20px] flex items-center gap-1.5 text-[12px] font-medium border-[0.5px] border-white/20 transition-all"
        >
          <IconMaximize className="w-4 h-4" />
          <span>Xem ảnh</span>
        </button>

        {/* Counter Badge */}
        <div className="absolute bottom-3 right-3 bg-black/50 text-white px-2.5 py-1 rounded-[20px] text-[12px] font-medium leading-none">
          {activeIndex + 1} / {venue.images.length}
        </div>

        {/* Venue Info overlaid bottom-left */}
        <div className="absolute bottom-3 left-3 text-white right-16">
          <div className="inline-flex items-center gap-1 bg-white/20 px-2 py-0.5 rounded-[20px] text-[11px] font-medium border-[0.5px] border-white/10 mb-1.5">
            <IconBallFootball className="w-3.5 h-3.5" />
            <span>{venue.sport}</span>
          </div>
          <h1 className="text-[22px] font-medium leading-tight mb-1 truncate">
            {venue.name}
          </h1>
          <div className="flex items-center gap-2 text-[12px] font-normal text-white/90">
            <div className="flex items-center gap-0.5 text-[#f0a500]">
              <IconStar className="w-3.5 h-3.5 fill-[#f0a500] text-[#f0a500]" />
              <span className="font-medium">{venue.rating.toFixed(1)}</span>
            </div>
            <span className="text-white/40">|</span>
            <div className="flex items-center gap-1 truncate max-w-xs md:max-w-md">
              <IconMapPin className="w-3.5 h-3.5 shrink-0" />
              <span className="truncate">{venue.address}</span>
            </div>
          </div>
        </div>
      </div>

      {/* [B] THUMBNAIL STRIP — full width */}
      <div className="grid grid-cols-5 gap-[3px] h-[64px] w-full bg-black">
        {venue.images.slice(0, 5).map((img, i) => {
          const isFifth = i === 4
          const hasMore = venue.images.length > 5
          const isActive = i === activeIndex
          const activeStyle = isActive ? { boxShadow: 'inset 0 0 0 2.5px #1a8a4a' } : undefined

          return (
            <div 
              key={i} 
              onClick={() => {
                if (isFifth && hasMore) {
                  openLightbox(4)
                } else {
                  setActiveIndex(i)
                }
              }}
              style={activeStyle}
              className="relative h-[64px] cursor-pointer bg-[#1e4535] overflow-hidden"
            >
              {img ? (
                <Image 
                  src={img} 
                  alt={`Thumbnail ${i + 1}`} 
                  fill
                  className="object-cover"
                  unoptimized
                />
              ) : (
                <div className="w-full h-full bg-[#1e4535] flex items-center justify-center text-[10px] text-white/40">
                  Sân {i + 1}
                </div>
              )}
              {isFifth && hasMore && (
                <div className="absolute inset-0 bg-black/60 flex items-center justify-center text-white font-medium text-[13px]">
                  +{venue.images.length - 4}
                </div>
              )}
            </div>
          )
        })}
      </div>

      {/* [C] BODY — stretches to fill screen */}
      <div className="w-full px-6 py-5 grid grid-cols-1 md:grid-cols-[1fr_300px] gap-5">
        
        {/* Left column (flex:1) — Tabs + Tab panel content */}
        <div className="flex flex-col min-w-0">
          
          {/* TAB BAR */}
          <div className="grid grid-cols-5 bg-white border-[0.5px] border-gray-200 rounded-[12px] overflow-hidden mb-3.5">
            {[
              { id: 'overview', icon: IconInfoCircle, label: 'Tổng quan' },
              { id: 'slots', icon: IconClock, label: 'Khung giờ' },
              { id: 'services', icon: IconPackage, label: 'Dịch vụ' },
              { id: 'location', icon: IconMap2, label: 'Vị trí' },
              { id: 'reviews', icon: IconMessageCircle, label: 'Đánh giá' }
            ].map((tab) => {
              const TabIcon = tab.icon
              const isActive = tab.id === activeTab
              const activeStyle = isActive 
                ? 'text-[#1a8a4a] border-b-[2px] border-[#1a8a4a]' 
                : 'text-gray-400 border-b-[2px] border-transparent hover:bg-gray-50'

              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`py-2 flex flex-col items-center justify-center transition-all ${activeStyle}`}
                  type="button"
                >
                  <TabIcon className="w-[19px] h-[19px] shrink-0" />
                  <span className="text-[11px] font-medium mt-0.5">{tab.label}</span>
                </button>
              )
            })}
          </div>

          {/* PANEL WRAPPER */}
          <div className="bg-white border-[0.5px] border-gray-200 rounded-[12px] p-4 min-h-[300px]">
            
            {/* TAB 1: Tổng quan */}
            {activeTab === 'overview' && (
              <div className="space-y-4">
                
                {/* Info Cards Grid */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-[9px]">
                  <div className="bg-gray-50 rounded-[8px] p-[11px_12px] flex items-center gap-3 border-[0.5px] border-gray-100">
                    <IconClock className="w-5 h-5 text-[#1a8a4a] shrink-0" />
                    <div className="leading-tight">
                      <span className="block text-[11px] text-gray-400 font-normal">Giờ mở cửa</span>
                      <span className="text-[13px] font-medium text-gray-700">{venue.openTime} – {venue.closeTime}</span>
                    </div>
                  </div>
                  <div className="bg-gray-50 rounded-[8px] p-[11px_12px] flex items-center gap-3 border-[0.5px] border-gray-100">
                    <IconCategory className="w-5 h-5 text-[#1a8a4a] shrink-0" />
                    <div className="leading-tight">
                      <span className="block text-[11px] text-gray-400 font-normal">Loại sân</span>
                      <span className="text-[13px] font-medium text-gray-700">{venue.sport}</span>
                    </div>
                  </div>
                  <div className="bg-gray-50 rounded-[8px] p-[11px_12px] flex items-center gap-3 border-[0.5px] border-gray-100">
                    <IconCircleCheck className="w-5 h-5 text-[#1a8a4a] shrink-0" />
                    <div className="leading-tight">
                      <span className="block text-[11px] text-gray-400 font-normal">Trạng thái</span>
                      <span className="text-[13px] font-medium text-[#1a8a4a]">Hoạt động</span>
                    </div>
                  </div>
                </div>

                {/* Description Box */}
                <div className="bg-gray-50 rounded-[8px] p-[11px_13px] border-[0.5px] border-gray-100">
                  <p className="text-[13px] text-gray-500 leading-[1.6] font-normal">
                    {venue.description || 'Chưa có mô tả cho sân này.'}
                  </p>
                </div>

                {/* Amenities */}
                <div className="space-y-2 pt-1">
                  <span className="block text-[11px] font-medium tracking-[0.5px] uppercase text-gray-400">
                    TIỆN ÍCH
                  </span>
                  <div className="flex flex-wrap gap-2">
                    {venue.amenities && venue.amenities.length > 0 ? (
                      venue.amenities.map((amenity, i) => (
                        <div 
                          key={i} 
                          className="inline-flex items-center bg-gray-50 border-[0.5px] border-gray-200 rounded-[20px] px-3 py-1.5 text-[12px] font-normal text-gray-600"
                        >
                          {getAmenityIcon(amenity)}
                          <span>{amenity}</span>
                        </div>
                      ))
                    ) : (
                      <div className="text-[12px] text-gray-400 py-1 italic">Chưa có thông tin tiện ích.</div>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* TAB 2: Khung giờ */}
            {activeTab === 'slots' && (
              <div className="space-y-3">
                {/* Header Row */}
                <div className="flex justify-between items-center px-0.5">
                  {/* Date picker trigger */}
                  <div className="relative" ref={datePickerRef}>
                    <button
                      onClick={() => setPopoverOpen(prev => !prev)}
                      className="bg-gray-50 border-[0.5px] border-gray-200 rounded-[8px] py-1.5 px-3 flex items-center gap-1.5 text-gray-700 hover:bg-gray-100 transition-colors"
                      type="button"
                    >
                      <IconCalendar className="w-[15px] h-[15px] text-[#1a8a4a] shrink-0" />
                      <span className="text-[13px] font-medium">
                        {formatDatePickerLabel(selectedDate)}
                      </span>
                      <span className="text-[9px] text-gray-400">
                        {popoverOpen ? '▲' : '▼'}
                      </span>
                    </button>

                    {popoverOpen && (
                      <div className="absolute top-[calc(100%+6px)] left-0 bg-white border-[0.5px] border-gray-200 rounded-[12px] p-3 w-[248px] z-50 shadow-none">
                        {/* Month selection header */}
                        <div className="flex justify-between items-center mb-2.5">
                          <span className="text-[13px] font-medium text-gray-700 select-none">
                            {getMonthYearLabel(viewMonth, viewYear)}
                          </span>
                          <div className="flex gap-1">
                            <button
                              onClick={handlePrevMonth}
                              disabled={isCurrentMonth}
                              type="button"
                              className="w-6 h-6 rounded-full flex items-center justify-center hover:bg-gray-100 transition-colors disabled:opacity-30 disabled:pointer-events-none"
                            >
                              <IconChevronLeft className="w-3.5 h-3.5 text-gray-600" />
                            </button>
                            <button
                              onClick={handleNextMonth}
                              type="button"
                              className="w-6 h-6 rounded-full flex items-center justify-center hover:bg-gray-100 transition-colors"
                            >
                              <IconChevronRight className="w-3.5 h-3.5 text-gray-600" />
                            </button>
                          </div>
                        </div>

                        {/* Day of Week Row */}
                        <div className="grid grid-cols-7 gap-[2px] text-center mb-1">
                          {['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'].map((day, idx) => (
                            <span key={idx} className="text-[10px] text-gray-400 font-medium select-none">
                              {day}
                            </span>
                          ))}
                        </div>

                        {/* Day Grid */}
                        <div className="grid grid-cols-7 gap-[2px]">
                          {generateCalendarDays(viewYear, viewMonth).map((day, i) => {
                            const isPast = isPastDay(day)
                            const isOtherMonth = day.getMonth() !== viewMonth
                            const isToday = isTodayDay(day)
                            const isSelected = isSelectedDay(day)
                            
                            let cellClass = "w-full text-center text-[12px] rounded-[5px] py-[5px] px-[2px] transition-colors select-none "
                            let clickHandler = () => {
                              setSelectedDate(day)
                              setPopoverOpen(false)
                            }

                            if (isPast) {
                              cellClass += "text-gray-300 cursor-not-allowed pointer-events-none font-normal"
                              clickHandler = () => {}
                            } else if (isSelected) {
                              cellClass += "bg-[#1a8a4a] text-white font-medium"
                            } else if (isToday) {
                              cellClass += "border border-[#1a8a4a] text-[#1a8a4a] font-medium"
                            } else if (isOtherMonth) {
                              cellClass += "text-gray-350 hover:bg-gray-100 cursor-pointer font-normal"
                            } else {
                              cellClass += "text-gray-700 hover:bg-gray-100 cursor-pointer font-normal"
                            }

                            return (
                              <button
                                key={i}
                                onClick={clickHandler}
                                disabled={isPast}
                                className={cellClass}
                                type="button"
                              >
                                {day.getDate()}
                              </button>
                            )
                          })}
                        </div>

                        {/* Footer "Hôm nay" button */}
                        <div className="border-t border-gray-100 mt-2.5 pt-2 flex justify-center">
                          <button
                            onClick={handleSelectToday}
                            className="text-[12px] font-medium text-[#1a8a4a] hover:bg-[#e8f7ee] py-1 px-3 rounded-[8px] transition-colors"
                            type="button"
                          >
                            Hôm nay
                          </button>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Available count label */}
                  <span className="text-[12px] font-medium text-[#1a8a4a]">
                    {availableSlotsCount} / 16 khung trống
                  </span>
                </div>

                {/* Today Notice Bar */}
                {isTodayDay(selectedDate) && (
                  <div className="bg-[#fffbeb] border-[0.5px] border-[#f5d87a] rounded-[8px] p-[7px_11px] flex items-start gap-2">
                    <IconInfoCircle className="w-4 h-4 text-[#7a5800] shrink-0 mt-0.5" />
                    <span className="text-[12px] text-[#7a5800] font-normal leading-normal">
                      Hôm nay — các khung giờ đã qua bị khóa tự động.
                    </span>
                  </div>
                )}

                {/* Slots Grid */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-[7px]">
                  {dateSlots.map((slot, i) => {
                    let cardClass = "flex items-center justify-between px-[11px] py-[9px] rounded-[8px] border-[0.5px] "
                    let clickHandler = () => setSelectedSlot(slot.hourStr)

                    if (slot.isPast) {
                      cardClass += "bg-gray-50 border-gray-150 text-gray-400 cursor-not-allowed pointer-events-none"
                      clickHandler = () => {}
                    } else if (!slot.available) {
                      cardClass += "bg-[#fdf0f0] border-[#f5b7b7] text-[#8a1c1c] cursor-not-allowed pointer-events-none"
                      clickHandler = () => {}
                    } else {
                      const isSelected = selectedSlot === slot.hourStr
                      if (isSelected) {
                        cardClass += "bg-[#e8f7ee] border-[#1a8a4a] border-[1.5px] text-[#0d5c2e] cursor-pointer"
                      } else {
                        cardClass += "bg-[#e8f7ee] border-[#9edbb6] text-[#0d5c2e] hover:bg-[#d4f0e2] cursor-pointer transition-colors"
                      }
                    }

                    return (
                      <div
                        key={i}
                        onClick={clickHandler}
                        className={cardClass}
                      >
                        <span className="text-[12px] font-medium">{slot.timeLabel}</span>
                        <div className="flex items-center gap-1 text-[11px] font-medium shrink-0">
                          {slot.isPast ? (
                            <>
                              <IconClockOff className="w-3.5 h-3.5 text-gray-400" />
                              <span>Đã qua</span>
                            </>
                          ) : !slot.available ? (
                            <>
                              <IconCircleX className="w-3.5 h-3.5 text-[#8a1c1c]" />
                              <span>Đã đặt</span>
                            </>
                          ) : (
                            <>
                              <IconCircleCheck className="w-3.5 h-3.5 text-[#0d5c2e]" />
                              <span>Còn trống</span>
                            </>
                          )}
                        </div>
                      </div>
                    )
                  })}
                </div>

                {/* Legend */}
                <div className="flex items-center gap-4 mt-[11px] text-[11px] text-gray-400 font-medium pt-1 select-none">
                  <div className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-[#1a8a4a]" />
                    <span>Còn trống</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-[#8a1c1c]" />
                    <span>Đã đặt</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="w-2 h-2 rounded-full bg-gray-300" />
                    <span>Đã qua</span>
                  </div>
                </div>

              </div>
            )}

            {/* TAB 3: Dịch vụ */}
            {activeTab === 'services' && (
              <div className="space-y-3">
                <span className="block text-[11px] font-medium tracking-[0.5px] uppercase text-gray-400">
                  DỊCH VỤ & PHỤ KIỆN
                </span>
                <div className="flex flex-col gap-2">
                  {venue.services && venue.services.length > 0 ? (
                    venue.services.map((srv, i) => (
                      <div 
                        key={i}
                        className="bg-gray-50 border-[0.5px] border-gray-100 rounded-[8px] p-[10px_13px] flex justify-between items-center"
                      >
                        <div className="leading-tight">
                          <span className="block text-[13px] font-medium text-gray-700">{srv.name}</span>
                          <span className="text-[11px] text-gray-400 font-normal">Còn {srv.stock} sản phẩm</span>
                        </div>
                        <span className="text-[14px] font-medium text-[#1a8a4a]">
                          {srv.price.toLocaleString('vi-VN')}đ
                        </span>
                      </div>
                    ))
                  ) : (
                    <div className="py-8 text-center text-[12px] text-gray-450">
                      Không có dịch vụ đi kèm.
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* TAB 4: Vị trí */}
            {activeTab === 'location' && (
              <div className="space-y-4">
                {/* Map Wrapper */}
                <div className="border-[0.5px] border-gray-200 rounded-[8px] overflow-hidden bg-gray-55 text-center flex flex-col">
                  {mapLoaded && (
                    <VenueMap 
                      latitude={venue.coordinates.lat} 
                      longitude={venue.coordinates.lng} 
                      venueName={venue.name} 
                      height="h-[180px]"
                    />
                  )}
                  <div className="border-t-[0.5px] border-gray-200 p-3 flex items-start gap-1.5 text-left">
                    <IconMapPin className="w-[16px] h-[16px] text-[#1a8a4a] shrink-0 mt-0.5" />
                    <span className="text-[13px] text-gray-500 font-normal leading-normal">
                      {venue.address}
                    </span>
                  </div>
                </div>

                {/* Owner Contact section */}
                <div className="space-y-2">
                  <span className="block text-[11px] font-medium tracking-[0.5px] uppercase text-gray-400">
                    LIÊN HỆ CHỦ SÂN
                  </span>
                  <ContactCard />
                </div>
              </div>
            )}

            {/* TAB 5: Đánh giá */}
            {activeTab === 'reviews' && (
              <div className="space-y-6">
                
                {/* Rating score header */}
                <div className="flex items-center gap-4 bg-gray-50 border-[0.5px] border-gray-200 rounded-[12px] p-4 shadow-none">
                  <span className="text-[36px] font-medium text-gray-700 leading-none">
                    {venue.rating.toFixed(1)}
                  </span>
                  <div className="flex flex-col leading-tight">
                    <div className="flex items-center gap-0.5">
                      {renderStars(venue.rating)}
                    </div>
                    <span className="text-[12px] text-gray-400 font-normal mt-1.5">
                      {venue.reviewCount} đánh giá
                    </span>
                  </div>
                </div>

                {/* Reviews list or Empty State */}
                {venue.recentReviews && venue.recentReviews.length > 0 ? (
                  <div className="flex flex-col gap-3">
                    {venue.recentReviews.map((review) => (
                      <div key={review.reviewId} className="bg-gray-50 border-[0.5px] border-gray-200 rounded-[10px] p-4 flex flex-col gap-2">
                        <div className="flex items-center justify-between gap-2">
                          <div className="flex items-center gap-2">
                            {review.userAvatar ? (
                              <Image 
                                src={review.userAvatar} 
                                alt={review.userName} 
                                width={32} 
                                height={32} 
                                className="rounded-full object-cover" 
                                unoptimized
                              />
                            ) : (
                              <div className="w-8 h-8 rounded-full bg-[#d4f0e2] text-[#1a8a4a] font-medium text-[12px] flex items-center justify-center uppercase">
                                {review.userName?.charAt(0) || 'U'}
                              </div>
                            )}
                            <span className="text-[13px] font-medium text-gray-700">{review.userName}</span>
                          </div>
                          <div className="flex items-center gap-0.5">
                            {renderStars(review.ratingScore, 13)}
                          </div>
                        </div>
                        <p className="text-[13px] text-gray-600 leading-relaxed">{review.comment}</p>
                        {review.ownerResponse && (
                          <div className="bg-white border-[0.5px] border-[#9edbb6] rounded-[8px] px-3 py-2 mt-1">
                            <span className="block text-[11px] font-medium text-[#1a8a4a] mb-0.5">Phản hồi của chủ sân</span>
                            <p className="text-[12px] text-gray-600">{review.ownerResponse}</p>
                          </div>
                        )}
                        <span className="text-[11px] text-gray-400">
                          {new Date(review.createdAt).toLocaleDateString('vi-VN')}
                        </span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-[24px] px-4 text-center select-none">
                    <div className="w-[54px] h-[54px] rounded-full bg-gray-50 border-[0.5px] border-gray-200 flex items-center justify-center mb-3">
                      <IconMessageOff className="w-[28px] h-[28px] text-gray-300" />
                    </div>
                    <p className="text-[13px] text-gray-500 font-medium mb-0.5">Chưa có đánh giá nào.</p>
                    <p className="text-[12px] text-gray-400 font-normal">Hãy là người đầu tiên đánh giá sân này!</p>
                  </div>
                )}

              </div>
            )}

          </div>
        </div>

        {/* Right column (300px) — Sticky sidebar */}
        <div className="w-full md:w-[300px] flex flex-col gap-[14px] shrink-0">
          
          <div className="sticky top-5 flex flex-col gap-[14px]">
            
            {/* CARD 1: Booking card */}
            <div className="bg-white border-[0.5px] border-gray-200 rounded-[12px] p-4 flex flex-col gap-3.5 shadow-none">
              
              <div>
                <span className="block text-[11px] text-gray-400 font-normal">Giá từ</span>
                <div className="flex items-baseline gap-0.5 mt-0.5">
                  <span className="text-[26px] font-medium text-[#1a8a4a] leading-none">
                    {venue.pricePerHour.toLocaleString('vi-VN')}đ
                  </span>
                  <span className="text-[13px] text-gray-400 font-normal">/giờ</span>
                </div>
              </div>

              {/* Rating chip */}
              <div className="flex items-center gap-1.5 bg-[#fffbeb] border-[0.5px] border-[#e8c84a] rounded-[8px] px-3 py-1.5 select-none">
                <IconStar className="w-[14px] h-[14px] fill-[#f0a500] text-[#f0a500]" />
                <span className="text-[14px] font-medium text-[#7a5800] leading-none">
                  {venue.rating.toFixed(1)} / 5.0
                </span>
                <span className="text-[11px] text-[#7a5800]/70 font-normal leading-none">
                  • {venue.reviewCount} đánh giá
                </span>
              </div>

              {/* CTA Booking Button */}
              <button 
                onClick={handleBookingRedirect}
                className="w-full flex items-center justify-center gap-1.5 bg-[#1a8a4a] hover:bg-[#157a3e] text-white font-medium text-[14px] py-3 rounded-[8px] transition-colors border-none"
                type="button"
              >
                <IconCalendarPlus className="w-[18px] h-[18px]" />
                <span>Đặt sân ngay</span>
              </button>

              <div className="border-t-[0.5px] border-gray-200 pt-3 flex flex-col gap-2">
                <span className="block text-[11px] font-medium tracking-[0.5px] uppercase text-gray-400">
                  THÔNG TIN NHANH
                </span>
                <div className="flex items-center justify-between text-[13px]">
                  <span className="text-gray-400">Loại sân</span>
                  <span className="text-gray-700 font-medium">{venue.sport}</span>
                </div>
                <div className="flex items-center justify-between text-[13px]">
                  <span className="text-gray-400">Trạng thái</span>
                  <span className="bg-[#e8f7ee] text-[#0d5c2e] text-[12px] font-medium px-2.5 py-0.5 rounded-[20px] select-none">
                    Hoạt động
                  </span>
                </div>
                <div className="flex items-center justify-between text-[13px]">
                  <span className="text-gray-400">Khung giờ trống</span>
                  <span className="text-[#1a8a4a] font-medium">{availableSlotsCount} / 16</span>
                </div>
              </div>

            </div>

            {/* CARD 2: Contact card */}
            <div className="bg-white border-[0.5px] border-gray-200 rounded-[12px] p-4 flex flex-col gap-2 shadow-none">
              <span className="block text-[11px] font-medium tracking-[0.5px] uppercase text-gray-400 select-none mb-1">
                LIÊN HỆ CHỦ SÂN
              </span>
              <ContactCard />
            </div>

          </div>

        </div>

      </div>

      </div> {/* Closing the max-w-5xl container */}

      {/* LIGHTBOX (full-screen image viewer) */}
      {lightboxOpen && (
        <div 
          className="fixed inset-0 z-50 flex flex-col items-center justify-center p-4 select-none"
          style={{ backgroundColor: 'rgba(0, 0, 0, 0.88)' }}
        >
          <div className="relative max-w-[720px] w-full flex flex-col items-center">
            
            {/* Top-right close button */}
            <button 
              onClick={() => setLightboxOpen(false)}
              className="absolute -top-12 right-2 w-9 h-9 rounded-full bg-black/60 hover:bg-black/80 text-white flex items-center justify-center border border-white/10 transition-colors z-20"
              type="button"
            >
              <IconX className="w-5 h-5" />
            </button>

            {/* Image display */}
            <div className="w-full h-[400px] bg-black rounded-[12px] overflow-hidden flex items-center justify-center border border-white/10 relative">
              {venue.images[lightboxIndex] ? (
                <Image 
                  src={venue.images[lightboxIndex]} 
                  alt={`Viewer ${lightboxIndex + 1}`} 
                  fill
                  className="object-cover"
                  unoptimized
                />
              ) : (
                <div className="w-full h-full bg-[#1e4535] flex items-center justify-center text-white/50 text-[14px]">
                  Hình ảnh sân
                </div>
              )}
            </div>

            {/* Caption */}
            <div className="mt-3 text-center">
              <p className="text-[12px] font-medium text-white/70">
                Ảnh {lightboxIndex + 1} / {venue.images.length} — {IMAGE_LABELS[lightboxIndex] || 'Chi tiết hình ảnh sân'}
              </p>
            </div>

            {/* Navigation buttons */}
            <div className="flex gap-4 justify-center mt-4">
              <button 
                onClick={() => setLightboxIndex(prev => (prev > 0 ? prev - 1 : venue.images.length - 1))}
                className="flex items-center gap-1.5 bg-white/10 hover:bg-white/20 active:bg-white/30 text-white text-[13px] font-medium px-4 py-2 rounded-[8px] transition-all border border-white/10"
                type="button"
              >
                <IconArrowLeft className="w-4 h-4" />
                <span>Trước</span>
              </button>
              <button 
                onClick={() => setLightboxIndex(prev => (prev < venue.images.length - 1 ? prev + 1 : 0))}
                className="flex items-center gap-1.5 bg-white/10 hover:bg-white/20 active:bg-white/30 text-white text-[13px] font-medium px-4 py-2 rounded-[8px] transition-all border border-white/10"
                type="button"
              >
                <span>Sau</span>
                <IconArrowRight className="w-4 h-4" />
              </button>
            </div>

            {/* Dot indicators */}
            <div className="flex gap-1.5 justify-center mt-5">
              {venue.images.map((_, i) => (
                <button 
                  key={i} 
                  onClick={() => setLightboxIndex(i)}
                  className={`h-[7px] rounded-full cursor-pointer transition-all duration-300 ${
                    i === lightboxIndex 
                      ? 'w-[20px] bg-white' 
                      : 'w-[7px] bg-white/30 hover:bg-white/50'
                  }`}
                  type="button"
                />
              ))}
            </div>

          </div>
        </div>
      )}

    </div>
  )
}
