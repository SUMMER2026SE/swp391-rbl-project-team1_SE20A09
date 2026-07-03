'use client'

import { useEffect, useRef, useCallback } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { ChatMessageDto, ConversationDto } from '@/lib/chat-api'

export interface TypingEvent {
  userId: number
  userName: string
  isTyping: boolean
}

export interface BlockEvent {
  userId: number
  blocked: boolean
  blockedByThem: boolean
  mutual: boolean
}

interface UseChatWebSocketOptions {
  /** Current user's ID — messages are received on /topic/chat/user/{userId} */
  userId: number | null
  /** Called when a new chat message arrives in real-time */
  onMessage: (message: ChatMessageDto) => void
  /** Called when a typing indicator event arrives */
  onTyping?: (event: TypingEvent) => void
  /** Called when a new group chat is automatically created */
  onNewGroup?: (conversation: ConversationDto) => void
  /** Called when a message is recalled by its sender */
  onMessageRecalled?: (message: ChatMessageDto) => void
  /** Called when a group chat is renamed */
  onGroupRenamed?: (conversation: ConversationDto) => void
  /** Called when block status changes */
  onBlockStatus?: (event: BlockEvent) => void
}

/**
 * Hook that manages a STOMP WebSocket connection for real-time chat.
 * - Subscribes to the current user's personal message topic
 * - Subscribes to typing indicator events
 * - Subscribes to new group, message recall, and group rename events
 * - Provides a sendTypingIndicator function
 * - Automatically reconnects on disconnect
 * - Cleans up on unmount
 */
export function useChatWebSocket({ userId, onMessage, onTyping, onNewGroup, onMessageRecalled, onGroupRenamed, onBlockStatus }: UseChatWebSocketOptions) {
  const clientRef = useRef<Client | null>(null)
  const onMessageRef = useRef(onMessage)
  const onTypingRef = useRef(onTyping)
  const onNewGroupRef = useRef(onNewGroup)
  const onMessageRecalledRef = useRef(onMessageRecalled)
  const onGroupRenamedRef = useRef(onGroupRenamed)
  const onBlockStatusRef = useRef(onBlockStatus)
  onMessageRef.current = onMessage
  onTypingRef.current = onTyping
  onNewGroupRef.current = onNewGroup
  onMessageRecalledRef.current = onMessageRecalled
  onGroupRenamedRef.current = onGroupRenamed
  onBlockStatusRef.current = onBlockStatus

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
              const parsed = JSON.parse(msg.body)
              if (parsed.type === 'message_read') {
                if (typeof window !== 'undefined') {
                  window.dispatchEvent(new CustomEvent('message_read', { detail: parsed }))
                }
              } else {
                onMessageRef.current(parsed as ChatMessageDto)
              }
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

        // Subscribe to new group chat creation
        client.subscribe(
          `/topic/chat/user/${userId}/new-group`,
          (msg: IMessage) => {
            try {
              const group: ConversationDto = JSON.parse(msg.body)
              onNewGroupRef.current?.(group)
            } catch {
              // malformed message — ignore
            }
          }
        )

        // Subscribe to message recall events
        client.subscribe(
          `/topic/chat/user/${userId}/message-recalled`,
          (msg: IMessage) => {
            try {
              const message: ChatMessageDto = JSON.parse(msg.body)
              onMessageRecalledRef.current?.(message)
            } catch {
              // malformed message — ignore
            }
          }
        )

        // Subscribe to group rename events
        client.subscribe(
          `/topic/chat/user/${userId}/group-renamed`,
          (msg: IMessage) => {
            try {
              const conv: ConversationDto = JSON.parse(msg.body)
              onGroupRenamedRef.current?.(conv)
            } catch {
              // malformed message — ignore
            }
          }
        )

        // Subscribe to message status events (e.g. hidden)
        client.subscribe(
          `/topic/chat/user/${userId}/message-status`,
          (msg: IMessage) => {
            try {
              const event = JSON.parse(msg.body)
              if (event.type === 'message_hidden' && event.messageId) {
                // Reuse message recalled callback since it updates UI
                onMessageRecalledRef.current?.({ messageId: event.messageId, content: '' } as ChatMessageDto)
              }
            } catch {
              // malformed message — ignore
            }
          }
        )

        // Subscribe to conversation status events (e.g. hidden)
        client.subscribe(
          `/topic/chat/user/${userId}/conversation-status`,
          (msg: IMessage) => {
            try {
              const event = JSON.parse(msg.body)
              if (event.type === 'conversation_hidden' && event.conversationId) {
                if (typeof window !== 'undefined') {
                  const currentEvent = new CustomEvent('conversation_hidden', { detail: { conversationId: event.conversationId } });
                  window.dispatchEvent(currentEvent);
                }
              }
            } catch {
              // malformed message — ignore
            }
          }
        )

        // Subscribe to block status events
        client.subscribe(
          `/topic/chat/user/${userId}/block-status`,
          (msg: IMessage) => {
            try {
              const event: BlockEvent = JSON.parse(msg.body)
              onBlockStatusRef.current?.(event)
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
