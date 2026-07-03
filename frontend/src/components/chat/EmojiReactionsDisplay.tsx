import { X } from "lucide-react"

interface EmojiReactionsDisplayProps {
  reactions: Record<string, number[]>
  currentUserId: number | undefined
  isMe: boolean
  onRemoveReaction: (emoji: string) => void
}

export function EmojiReactionsDisplay({
  reactions,
  currentUserId,
  isMe,
  onRemoveReaction
}: EmojiReactionsDisplayProps) {
  if (!reactions || Object.keys(reactions).length === 0) return null

  return (
    <div className={`flex flex-wrap gap-1 mt-1.5 ${isMe ? 'justify-end' : 'justify-start'}`}>
      {Object.entries(reactions).map(([emoji, userIds]) => {
        if (!userIds || userIds.length === 0) return null
        
        const hasMyReaction = currentUserId && userIds.includes(currentUserId)

        return (
          <div 
            key={emoji} 
            className="group/emoji relative bg-background/90 text-foreground border shadow-sm rounded-full px-1.5 py-0.5 text-[11px] flex items-center gap-1 cursor-default"
          >
            <span>{emoji}</span>
            <span className="font-medium">{userIds.length}</span>
            
            {hasMyReaction && (
              <button
                onClick={() => onRemoveReaction(emoji)}
                title="Xóa phản ứng"
                className="absolute -top-1 -right-1 bg-background border shadow-sm rounded-full p-0.5 opacity-0 group-hover/emoji:opacity-100 transition-opacity hover:bg-muted text-muted-foreground hover:text-foreground"
              >
                <X className="h-2 w-2" />
              </button>
            )}
          </div>
        )
      })}
    </div>
  )
}
