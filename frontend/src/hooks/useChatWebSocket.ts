'use client'

import { useEffect, useRef, useCallback } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { ChatMessageDto } from '@/lib/chat-api'

export interface TypingEvent {
  userId: number
  userName: string
  isTyping: boolean
}

interface UseChatWebSocketOptions {
  /** Current user's ID — messages are received on /topic/chat/user/{userId} */
  userId: number | null
  /** Called when a new chat message arrives in real-time */
  onMessage: (message: ChatMessageDto) => void
  /** Called when a typing indicator event arrives */
  onTyping?: (event: TypingEvent) => void
}

/**
 * Hook that manages a STOMP WebSocket connection for real-time chat.
 * - Subscribes to the current user's personal message topic
 * - Subscribes to typing indicator events
 * - Provides a sendTypingIndicator function
 * - Automatically reconnects on disconnect
 * - Cleans up on unmount
 */
export function useChatWebSocket({ userId, onMessage, onTyping }: UseChatWebSocketOptions) {
  const clientRef = useRef<Client | null>(null)
  const onMessageRef = useRef(onMessage)
  const onTypingRef = useRef(onTyping)
  onMessageRef.current = onMessage
  onTypingRef.current = onTyping

  useEffect(() => {
    if (!userId) return

    const rawBase = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'
    const backendBase = rawBase
      .replace(/\/api\/v1\/?$/, '')
      .replace(/^wss?:\/\//, 'http://')
    const sockJsUrl = `${backendBase}/ws`

    // Retrieve JWT token for WebSocket auth
    let token: string | null = null
    if (typeof window !== 'undefined') {
      token = localStorage.getItem('access_token')
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(sockJsUrl),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        // Subscribe to personal message topic
        client.subscribe(
          `/topic/chat/user/${userId}`,
          (msg: IMessage) => {
            try {
              const message: ChatMessageDto = JSON.parse(msg.body)
              onMessageRef.current(message)
            } catch {
              // malformed message — ignore
            }
          }
        )

        // Subscribe to typing indicators
        client.subscribe(
          `/topic/chat/typing/${userId}`,
          (msg: IMessage) => {
            try {
              const event: TypingEvent = JSON.parse(msg.body)
              onTypingRef.current?.(event)
            } catch {
              // malformed message — ignore
            }
          }
        )
      },
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
      clientRef.current = null
    }
  }, [userId])

  /**
   * Send a typing indicator to a specific recipient.
   */
  const sendTypingIndicator = useCallback(
    (recipientId: number, isTyping: boolean) => {
      const client = clientRef.current
      if (client?.connected) {
        client.publish({
          destination: '/app/chat.typing',
          body: JSON.stringify({ recipientId, isTyping }),
        })
      }
    },
    []
  )

  return { sendTypingIndicator }
}
