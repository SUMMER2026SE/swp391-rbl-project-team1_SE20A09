import { X, Reply } from "lucide-react"
import type { ChatMessageDto } from "@/lib/chat-api"

interface QuoteBoxProps {
  message: ChatMessageDto
  onCancel: () => void
}

export function QuoteBox({ message, onCancel }: QuoteBoxProps) {
  return (
    <div className="flex items-start gap-3 bg-muted/50 px-4 py-2 border-t">
      <div className="mt-1 text-primary">
        <Reply className="h-4 w-4" />
      </div>
      <div className="flex-1 min-w-0 border-l-2 border-primary pl-3">
        <p className="text-xs font-semibold text-primary mb-0.5">
          Đang trả lời {message.senderName}
        </p>
        <p className="text-sm text-muted-foreground truncate">
          {message.content}
        </p>
      </div>
      <button 
        onClick={onCancel}
        className="text-muted-foreground hover:text-foreground transition-colors p-1"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  )
}
