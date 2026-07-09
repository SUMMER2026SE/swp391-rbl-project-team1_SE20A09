import { get, post, put, del } from '@/lib/api'

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
  blocked?: boolean
  blockedByThem?: boolean
  blockedByUserId?: number | null
  leftGroup?: boolean
}

export interface BlockStatus {
  userId: number
  blocked: boolean
  blockedByThem: boolean
  mutual: boolean
}

export interface ChatMessageDto {
  messageId: number
  conversationId: number
  senderId: number
  senderName: string
  senderAvatar: string | null
  content: string
  timestamp: string
  sentAt?: string
  isRead: boolean
  messageType: 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM'
  action?: 'left_group'
  // New fields for advanced features (UI only for now)
  reactions?: Record<string, number[]> // Map emoji to array of userIds
  pinnedBy?: number[] // Array of user IDs who pinned this message
  quotedMessageId?: number
  quotedMessageContent?: string
  readByAvatars?: string[]
  readByNames?: string[]
  forwarded?: boolean
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

export async function getMatchConversation(matchId: number): Promise<{ conversationId: number }> {
  return get<{ conversationId: number }>(`/chat/matches/${matchId}/conversation`)
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

export async function renameGroupChat(conversationId: number, name: string): Promise<ConversationDto> {
  return put<ConversationDto>(`/chat/conversations/${conversationId}/rename`, { name })
}

export async function recallMessage(messageId: number): Promise<ChatMessageDto> {
  return del<ChatMessageDto>(`/chat/messages/${messageId}/recall`)
}

export async function hideMessage(messageId: number): Promise<void> {
  return del<void>(`/chat/messages/${messageId}/hide`)
}

export async function blockUser(userId: number): Promise<BlockStatus> {
  return post<BlockStatus>(`/chat/users/${userId}/block`, {})
}

export async function unblockUser(userId: number): Promise<BlockStatus> {
  return del<BlockStatus>(`/chat/users/${userId}/block`)
}

export async function leaveGroupChat(conversationId: number): Promise<void> {
  return post<void>(`/chat/conversations/${conversationId}/leave`, {})
}

export async function deleteConversation(conversationId: number): Promise<void> {
  return del<void>(`/chat/conversations/${conversationId}`)
}
