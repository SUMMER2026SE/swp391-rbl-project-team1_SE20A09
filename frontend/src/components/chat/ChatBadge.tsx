'use client'

import { useEffect } from 'react'
import Link from 'next/link'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { getUnreadCount } from '@/lib/chat-api'
import { useChatWebSocket } from '@/hooks/useChatWebSocket'
import { IconBrandMessenger } from "@tabler/icons-react"
import { Button } from '@/components/ui/button'

export function ChatBadge({ userId }: { userId: number | undefined }) {
  const queryClient = useQueryClient()
  
  const { data: countData, refetch } = useQuery({
    queryKey: ['chat', 'unread-count'],
    queryFn: () => getUnreadCount(),
    enabled: !!userId,
    refetchInterval: 30000, // Fallback polling
  })

  const unreadCount = countData?.unreadCount ?? 0

  useChatWebSocket({
    userId: userId ?? null,
    onMessage: (msg) => {
      // If receiving a message from someone else, we might have a new unread message.
      if (msg.senderId !== userId) {
        // Wait a tiny bit to allow any active chat page to mark it as read first,
        // preventing a race condition where we fetch before the read is committed.
        setTimeout(() => {
          refetch()
        }, 500)
      }
    },
  })

  return (
    <Link href="/chat">
      <Button variant="ghost" size="icon" className="relative h-12 w-12 rounded-full text-muted-foreground hover:bg-muted hover:text-primary transition-colors" aria-label="Tin nhắn">
        <IconBrandMessenger className="h-[36px] w-[36px]" stroke={1.75} />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 h-3.5 w-3.5 rounded-full bg-red-500 ring-2 ring-background animate-in zoom-in" />
        )}
      </Button>
    </Link>
  )
}
