import { get, post } from '@/lib/api'

// ── Types ────────────────────────────────────────────────────

export interface ConversationDto {
  conversationId: number | null
  isGroup?: boolean
  otherUserId: number | null
  otherUserName: string
  otherUserAvatar: string | null
  otherUserOnline: boolean
  lastMessagePreview: string | null
  lastMessageAt: string | null
  unreadCount: number
}

export interface ChatMessageDto {
  messageId: number
  conversationId: number
  senderId: number
  senderName: string
  senderAvatar: string | null
  content: string
  messageType: string
  isRead: boolean
  sentAt: string
}

export interface SendMessageRequest {
  recipientId?: number
  conversationId?: number
  content: string
  messageType?: string
}

export interface ChatbotResponse {
  reply: string
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  totalPages: number
  totalElements: number
  number: number
  size: number
  first: boolean
  last: boolean
}

// ── API Functions ────────────────────────────────────────────

export async function getConversations(): Promise<ConversationDto[]> {
  return get<ConversationDto[]>('/chat/conversations')
}

export async function getOrCreateConversation(recipientId: number): Promise<{ conversationId: number }> {
  return post<{ conversationId: number }>(`/chat/conversations/${recipientId}`)
}

export async function getMessages(
  conversationId: number,
  page = 0,
  size = 30
): Promise<PageResponse<ChatMessageDto>> {
  return get<PageResponse<ChatMessageDto>>(
    `/chat/conversations/${conversationId}/messages?page=${page}&size=${size}`
  )
}

export async function sendMessage(request: SendMessageRequest): Promise<ChatMessageDto> {
  return post<ChatMessageDto>('/chat/messages', request)
}

export async function markConversationAsRead(conversationId: number): Promise<void> {
  return post<void>(`/chat/conversations/${conversationId}/read`)
}

export async function getUnreadCount(): Promise<{ unreadCount: number }> {
  return get<{ unreadCount: number }>('/chat/unread-count')
}

export async function searchUsers(query: string): Promise<ConversationDto[]> {
  return get<ConversationDto[]>(`/chat/users/search?q=${encodeURIComponent(query)}`)
}

export async function sendChatbotQuery(message: string): Promise<ChatbotResponse> {
  return post<ChatbotResponse>('/chat/chatbot', { message })
}
