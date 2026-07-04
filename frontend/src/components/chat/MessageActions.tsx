import { Smile, Reply, MoreVertical, Copy, Pin, Trash2 } from "lucide-react"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import type { ChatMessageDto } from "@/lib/chat-api"

interface MessageActionsProps {
  message: ChatMessageDto
  isMe: boolean
  onReact: (emoji: string) => void
  onReply: () => void
  onForward: () => void
  onPin: () => void
  onRecall: () => void
}

const EMOJI_LIST = ['👍', '❤️', '😂', '😮', '😢', '😡']

export function MessageActions({
  message,
  isMe,
  onReact,
  onReply,
  onForward,
  onPin,
  onRecall
}: MessageActionsProps) {
  return (
    <div className={`flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity bg-background/50 backdrop-blur-sm border rounded-full px-1 py-1 shadow-sm ${isMe ? 'mr-2 flex-row-reverse' : 'ml-2'}`}>
      {/* Emoji Quick Picker */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button className="p-1.5 text-muted-foreground hover:bg-muted rounded-full hover:text-foreground transition-colors">
            <Smile className="h-4 w-4" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align={isMe ? "end" : "start"} side="top" className="flex gap-1 p-2 rounded-full w-auto">
          {EMOJI_LIST.map(emoji => (
            <button
              key={emoji}
              onClick={() => onReact(emoji)}
              className="text-xl hover:scale-125 transition-transform p-1"
            >
              {emoji}
            </button>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>

      {/* Reply */}
      <button 
        onClick={onReply}
        className="p-1.5 text-muted-foreground hover:bg-muted rounded-full hover:text-foreground transition-colors"
        title="Trả lời"
      >
        <Reply className="h-4 w-4" />
      </button>

      {/* More Actions */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button className="p-1.5 text-muted-foreground hover:bg-muted rounded-full hover:text-foreground transition-colors">
            <MoreVertical className="h-4 w-4" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align={isMe ? "end" : "start"} className="w-48">
          <DropdownMenuItem onClick={onForward} className="cursor-pointer gap-2">
            <Copy className="h-4 w-4" />
            <span>Chuyển tiếp</span>
          </DropdownMenuItem>
          <DropdownMenuItem onClick={onPin} className="cursor-pointer gap-2">
            <Pin className="h-4 w-4" />
            <span>Ghim</span>
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={onRecall} className="cursor-pointer gap-2 text-destructive focus:text-destructive focus:bg-destructive/10">
            <Trash2 className="h-4 w-4" />
            <span>Thu hồi</span>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  )
}
