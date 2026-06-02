'use client'

import React, { useEffect, useState } from 'react'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { 
  Bell, 
  CheckCheck, 
  Calendar, 
  CreditCard, 
  Info, 
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  Filter,
  Star,
  MessageSquare,
  ArrowRight
} from 'lucide-react'
import { format } from 'date-fns'
import { vi } from 'date-fns/locale'
import api from '@/lib/api'
import { ApiResponse, NotificationResponse, PageResponse, NotificationType } from '@/types/notification'
import { toast } from 'sonner'
import Link from 'next/link'

export default function OwnerNotificationsPage() {
  const [notifications, setNotifications] = useState<NotificationResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [unreadOnly, setUnreadOnly] = useState(false)
  const [totalElements, setTotalElements] = useState(0)

  const fetchNotifications = async (pageNumber: number, onlyUnread: boolean) => {
    setLoading(true)
    try {
      // Axios instance returns response.data which is ApiResponse
      // Data structure: response.data.result.content
      const response = await api.get<ApiResponse<PageResponse<NotificationResponse>>>(
        `/notifications?page=${pageNumber}&size=10&unreadOnly=${onlyUnread}`
      )
      
      const apiResult = response.data.result
      
      if (apiResult) {
        setNotifications(apiResult.content || [])
        setTotalPages(apiResult.totalPages || 0)
        setTotalElements(apiResult.totalElements || 0)
      }
    } catch (error: any) {
      console.error('Failed to fetch notifications:', error)
      toast.error(error.message || 'Không thể tải thông báo')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchNotifications(page, unreadOnly)
  }, [page, unreadOnly])

  const markAsRead = async (id: number) => {
    const previousNotifications = [...notifications]
    setNotifications(prev => 
      prev.map(n => n.notificationId === id ? { ...n, isRead: true } : n)
    )

    try {
      await api.patch(`/notifications/mark-as-read`, [id])
    } catch (error) {
      setNotifications(previousNotifications)
      toast.error('Không thể đánh dấu là đã đọc')
    }
  }

  const markAllAsRead = async () => {
    try {
      await api.patch(`/notifications/mark-all-as-read`)
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })))
      toast.success('Đã đánh dấu tất cả là đã đọc')
    } catch (error) {
      toast.error('Có lỗi xảy ra')
    }
  }

  const getIcon = (type: NotificationType) => {
    switch (type) {
      case NotificationType.BOOKING: return <Calendar className="h-5 w-5 text-blue-500" />
      case NotificationType.PAYMENT: return <CreditCard className="h-5 w-5 text-green-500" />
      case NotificationType.REVIEW: return <Star className="h-5 w-5 text-yellow-500" />
      case NotificationType.COMPLAINT: return <MessageSquare className="h-5 w-5 text-red-500" />
      case NotificationType.SYSTEM: return <AlertTriangle className="h-5 w-5 text-amber-500" />
      default: return <Info className="h-5 w-5 text-gray-500" />
    }
  }

  const getLink = (type: NotificationType, resourceId?: string) => {
    if (!resourceId) return '#'
    switch (type) {
      case NotificationType.BOOKING: return `/owner/bookings`
      case NotificationType.PAYMENT: return `/owner/revenue`
      case NotificationType.REVIEW: return `/owner/venues`
      case NotificationType.COMPLAINT: return `/owner/dashboard`
      default: return '#'
    }
  }

  return (
    <main className="container mx-auto mt-8 px-4 max-w-4xl">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4">
          <div>
            <h1 className="text-3xl font-extrabold flex items-center gap-3 text-foreground tracking-tight">
              <div className="bg-primary/10 p-2 rounded-xl">
                <Bell className="h-7 w-7 text-primary" />
              </div>
              Thông báo
            </h1>
            <p className="text-muted-foreground mt-2 font-medium">
              Bạn đang có <span className="text-primary">{totalElements}</span> thông báo {unreadOnly ? 'chưa đọc' : ''}
            </p>
          </div>
          
          <div className="flex gap-3 w-full md:w-auto">
            <Button 
              variant={unreadOnly ? "default" : "outline"} 
              size="sm"
              className="rounded-full px-5"
              onClick={() => { setUnreadOnly(!unreadOnly); setPage(0); }}
            >
              <Filter className="mr-2 h-4 w-4" />
              {unreadOnly ? 'Xem tất cả' : 'Chỉ chưa đọc'}
            </Button>
            <Button variant="outline" size="sm" className="rounded-full px-5" onClick={markAllAsRead}>
              <CheckCheck className="mr-2 h-4 w-4" />
              Đọc tất cả
            </Button>
          </div>
        </div>

        <Card className="border-none shadow-xl shadow-primary/5 overflow-hidden rounded-2xl">
          <CardContent className="p-0">
            {loading ? (
              <div className="p-6 space-y-6">
                {[...Array(5)].map((_, i) => (
                  <div key={i} className="flex gap-5">
                    <Skeleton className="h-12 w-12 rounded-2xl" />
                    <div className="space-y-3 flex-1">
                      <Skeleton className="h-5 w-1/3" />
                      <Skeleton className="h-4 w-full" />
                    </div>
                  </div>
                ))}
              </div>
            ) : notifications.length === 0 ? (
              <div className="p-20 text-center flex flex-col items-center justify-center">
                <div className="bg-muted/30 p-6 rounded-full mb-6">
                  <Bell className="h-16 w-16 text-muted-foreground/40" />
                </div>
                <h3 className="text-xl font-bold text-foreground">Hộp thư trống</h3>
                <p className="text-muted-foreground mt-2 max-w-xs">
                  Bạn đã cập nhật hết mọi tin tức! Khi có thông báo mới chúng tôi sẽ báo cho bạn.
                </p>
              </div>
            ) : (
              <div className="divide-y divide-border/50">
                {notifications.map((n) => (
                  <div 
                    key={n.notificationId} 
                    className={`group p-6 transition-all hover:bg-muted/30 flex gap-5 relative ${!n.isRead ? 'bg-primary/[0.03]' : ''}`}
                  >
                    {!n.isRead && (
                      <div className="absolute left-0 top-0 bottom-0 w-1 bg-primary rounded-r-full" />
                    )}
                    
                    <div className="flex-shrink-0 mt-1">
                      <div className="bg-white shadow-sm p-3 rounded-2xl border border-border/40 group-hover:scale-110 transition-transform">
                        {getIcon(n.notificationType)}
                      </div>
                    </div>
                    
                    <div className="flex-grow">
                      <div className="flex justify-between items-start mb-2">
                        <h4 className={`text-lg font-bold leading-tight ${!n.isRead ? 'text-primary' : 'text-foreground'}`}>
                          {n.title}
                        </h4>
                        <span className="text-xs font-semibold text-muted-foreground bg-muted px-2 py-1 rounded-md whitespace-nowrap ml-4">
                          {format(new Date(n.createdAt), 'HH:mm dd/MM', { locale: vi })}
                        </span>
                      </div>
                      
                      <p className="text-sm text-muted-foreground/80 leading-relaxed font-medium">
                        {n.message}
                      </p>
                      
                      <div className="mt-4 flex items-center justify-between">
                        <div className="flex gap-3">
                          {!n.isRead && (
                            <Button 
                              variant="secondary" 
                              size="sm" 
                              className="h-9 px-4 text-xs font-bold rounded-xl"
                              onClick={() => markAsRead(n.notificationId)}
                            >
                              Đánh dấu đã đọc
                            </Button>
                          )}
                          {n.relatedResourceId && (
                            <Link href={getLink(n.notificationType, n.relatedResourceId)}>
                              <Button variant="ghost" size="sm" className="h-9 px-4 text-xs font-bold rounded-xl text-primary hover:text-primary hover:bg-primary/10">
                                Xem chi tiết <ArrowRight className="ml-2 h-3 w-3" />
                              </Button>
                            </Link>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {totalPages > 1 && (
          <div className="flex justify-center items-center mt-10 gap-3">
            <Button
              variant="outline"
              size="icon"
              className="rounded-xl w-10 h-10"
              disabled={page === 0}
              onClick={() => {
                setPage(page - 1)
                window.scrollTo({ top: 0, behavior: 'smooth' })
              }}
            >
              <ChevronLeft className="h-5 w-5" />
            </Button>
            <div className="bg-muted px-4 py-2 rounded-xl text-sm font-bold">
              Trang {page + 1} / {totalPages}
            </div>
            <Button
              variant="outline"
              size="icon"
              className="rounded-xl w-10 h-10"
              disabled={page === totalPages - 1}
              onClick={() => {
                setPage(page + 1)
                window.scrollTo({ top: 0, behavior: 'smooth' })
              }}
            >
              <ChevronRight className="h-5 w-5" />
            </Button>
          </div>
          )}
          </main>
          );
          }
