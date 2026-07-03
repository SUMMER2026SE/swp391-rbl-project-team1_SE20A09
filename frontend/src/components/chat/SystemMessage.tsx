import type { ChatMessageDto } from "@/lib/chat-api"

interface SystemMessageProps {
  message: ChatMessageDto
}

export function SystemMessage({ message }: SystemMessageProps) {
  return (
    <div className="flex justify-center my-4 animate-fade-in-up">
      <div className="bg-transparent px-4 py-1.5 rounded-full">
        <p className="text-xs text-muted-foreground text-center font-medium">
          {message.content}
        </p>
      </div>
    </div>
  )
}
