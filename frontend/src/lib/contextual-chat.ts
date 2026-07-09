import { getOrCreateConversation, sendMessage } from '@/lib/chat-api'

export type ChatContext =
  | { action: 'stadium_referral'; stadiumId: number; stadiumName: string }
  | { action: 'match_referral'; matchId: number; title: string; sportName?: string; playDate?: string }
  | { action: 'booking_referral'; bookingId: number; stadiumName: string; playDate?: string; time?: string }

export async function createContextualConversation(recipientId: number, context: ChatContext) {
  const { conversationId } = await getOrCreateConversation(recipientId)
  await sendMessage({ conversationId, content: JSON.stringify(context), messageType: 'SYSTEM' })
  return conversationId
}

export function chatUrl(conversationId: number) {
  return `/chat?conversationId=${conversationId}`
}
