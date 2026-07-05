import { Pin } from "lucide-react"
import type { ChatMessageDto } from "@/lib/chat-api"

interface PinnedMessagesProps {
  messages: ChatMessageDto[]
}

export function PinnedMessages({ messages }: PinnedMessagesProps) {
  if (messages.length === 0) return null

  return (
    <div className="bg-muted/30 px-4 py-2 flex items-center gap-3 border-b border-t cursor-pointer hover:bg-muted/50 transition-colors">
      <div className="bg-primary/10 p-1.5 rounded-full text-primary">
        <Pin className="h-3.5 w-3.5" />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-medium text-primary">
          {messages.length} tin nhắn đã ghim
        </p>
        <p className="text-xs text-muted-foreground truncate">
          {messages[messages.length - 1].content}
        </p>
      </div>
    </div>
  )
}
