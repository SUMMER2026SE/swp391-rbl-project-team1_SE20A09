import { MoreHorizontal, MoreVertical, Ban, Eye, EyeOff, Trash2, LogOut } from "lucide-react"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import type { ConversationDto } from "@/lib/chat-api"

interface ConversationActionsProps {
  conv: ConversationDto
  iconType?: "horizontal" | "vertical"
  onToggleRead: (conv: ConversationDto) => void
  onToggleBlock: (conv: ConversationDto) => void
  onLeaveGroup?: (conv: ConversationDto) => void
  onDelete: (conv: ConversationDto) => void
}

export function ConversationActions({ conv, iconType = "horizontal", onToggleRead, onToggleBlock, onLeaveGroup, onDelete }: ConversationActionsProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button 
          onClick={(e) => e.stopPropagation()} 
          className="p-2 bg-transparent hover:bg-muted text-foreground transition-colors rounded-full"
        >
          {iconType === "horizontal" ? <MoreHorizontal className="h-4 w-4" /> : <MoreVertical className="h-4 w-4" />}
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuItem onClick={(e) => { e.stopPropagation(); onToggleRead(conv) }} className="cursor-pointer gap-2">
          {conv.unreadCount > 0 ? (
            <>
              <Eye className="h-4 w-4" />
              <span>Đánh dấu đã đọc</span>
            </>
          ) : (
            <>
              <EyeOff className="h-4 w-4" />
              <span>Đánh dấu chưa đọc</span>
            </>
          )}
        </DropdownMenuItem>
        
        {!conv.isGroup ? (
          <DropdownMenuItem onClick={(e) => { e.stopPropagation(); onToggleBlock(conv) }} className="cursor-pointer gap-2">
            <Ban className="h-4 w-4" />
            <span>{conv.blocked ? "Bỏ chặn" : "Chặn"}</span>
          </DropdownMenuItem>
        ) : !conv.leftGroup ? (
          <DropdownMenuItem onClick={(e) => { e.stopPropagation(); onLeaveGroup?.(conv) }} className="cursor-pointer gap-2 text-destructive focus:text-destructive focus:bg-destructive/10">
            <LogOut className="h-4 w-4" />
            <span>Rời khỏi nhóm</span>
          </DropdownMenuItem>
        ) : null}
        
        <DropdownMenuSeparator />
        
        <DropdownMenuItem onClick={(e) => { e.stopPropagation(); onDelete(conv) }} className="cursor-pointer gap-2 text-destructive focus:text-destructive focus:bg-destructive/10">
          <Trash2 className="h-4 w-4" />
          <span>Xóa đoạn chat</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
