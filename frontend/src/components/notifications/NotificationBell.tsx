'use client'

import { Bell } from 'lucide-react'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { get } from '@/lib/api'
import { ApiResponse } from '@/types/notification'

export function NotificationBell() {
  const { data: countData } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () => get<ApiResponse<number>>('/notifications/unread-count'),
    refetchInterval: 30000, // Tự động làm mới mỗi 30 giây
  })

  const count = countData?.result ?? 0

  return (
    <Link 
      href="/owner/notifications" 
      className="relative p-2 text-gray-600 hover:text-gray-900 transition-colors"
      title="Thông báo"
    >
      <Bell className="w-6 h-6" />
      {count > 0 && (
        <span className="absolute top-1 right-1 flex h-5 w-5 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white ring-2 ring-white">
          {count > 99 ? '99+' : count}
        </span>
      )}
    </Link>
  )
}
