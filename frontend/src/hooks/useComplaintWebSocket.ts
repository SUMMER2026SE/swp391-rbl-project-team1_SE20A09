'use client'

import { useEffect, useRef } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export interface ComplaintChatEvent {
  complaintId: number
  from: string
  message: string
  time: string
  newStatus: string
}

/**
 * Subscribes to real-time complaint chat events via STOMP over SockJS.
 * Connects once per mount; re-subscribes when complaintId changes.
 * Cleans up the STOMP client on unmount.
 */
export function useComplaintWebSocket(
  complaintId: number | null,
  onEvent: (event: ComplaintChatEvent) => void
) {
  const clientRef = useRef<Client | null>(null)
  const subscriptionRef = useRef<{ unsubscribe: () => void } | null>(null)
  const onEventRef = useRef(onEvent)
  onEventRef.current = onEvent

  useEffect(() => {
    if (!complaintId) return

    // Compute URL inside useEffect — SockJS requires http:// or https://, never ws://
    const rawBase = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'
    // Strip trailing /api/v1 path if present, then force http scheme
    const backendBase = rawBase
      .replace(/\/api\/v1\/?$/, '')
      .replace(/^wss?:\/\//, 'http://')
    const sockJsUrl = `${backendBase}/ws`

    const token = localStorage.getItem('access_token')

    const client = new Client({
      webSocketFactory: () => new SockJS(sockJsUrl),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        subscriptionRef.current?.unsubscribe()
        subscriptionRef.current = client.subscribe(
          `/topic/complaint/${complaintId}`,
          (msg: IMessage) => {
            try {
              const event: ComplaintChatEvent = JSON.parse(msg.body)
              onEventRef.current(event)
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
      subscriptionRef.current?.unsubscribe()
      subscriptionRef.current = null
      client.deactivate()
      clientRef.current = null
    }
  }, [complaintId])
}
