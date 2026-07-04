import { useState, useMemo } from "react"
import { Search, Users, Loader2 } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { ScrollArea } from "@/components/ui/scroll-area"
import type { ChatMessageDto, ConversationDto } from "@/lib/chat-api"

interface ForwardModalProps {
  message: ChatMessageDto | null
  conversations: ConversationDto[]
  isOpen: boolean
  onClose: () => void
  onForward: (message: ChatMessageDto, conversationId: number | null, recipientId: number | null) => void
}

export function ForwardModal({ message, conversations, isOpen, onClose, onForward }: ForwardModalProps) {
  const [searchQuery, setSearchQuery] = useState("")
  const [sendingTo, setSendingTo] = useState<Set<number>>(new Set())

  // Get top 10 recent conversations matching search
  const recentConversations = useMemo(() => {
    let filtered = conversations
    if (searchQuery.trim()) {
      filtered = conversations.filter(c => 
        c.otherUserName?.toLowerCase().includes(searchQuery.toLowerCase())
      )
    }
    return filtered.slice(0, 10)
  }, [conversations, searchQuery])

  const handleSend = async (conv: ConversationDto) => {
    if (!message) return
    const idToTrack = conv.conversationId || conv.otherUserId || 0
    
    setSendingTo(prev => new Set(prev).add(idToTrack))
    try {
      await onForward(message, conv.conversationId, conv.otherUserId)
      // Optional: keep it disabled to show it was sent
    } finally {
      // If we want to re-enable it immediately, we do this:
      // setSendingTo(prev => {
      //   const next = new Set(prev)
      //   next.delete(idToTrack)
      //   return next
      // })
    }
  }

  if (!message) return null

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-lg bg-background text-foreground border-border shadow-2xl p-0 overflow-hidden">
        <DialogHeader className="px-4 py-3 border-b border-border">
          <DialogTitle className="text-center text-xl font-bold">
            Chuyển tiếp
          </DialogTitle>
        </DialogHeader>

        <div className="p-4 border-b border-border">
          <div className="relative">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input 
              placeholder="Tìm kiếm người và nhóm" 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 h-10 bg-muted border-transparent text-foreground placeholder:text-muted-foreground focus-visible:ring-1 focus-visible:ring-ring rounded-full" 
            />
          </div>
        </div>

        <div className="px-4 py-2">
          <h4 className="text-sm font-semibold text-foreground/90 mb-2">Mới đây</h4>
        </div>

        <ScrollArea className="h-[300px]">
          {recentConversations.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              Không tìm thấy cuộc trò chuyện nào
            </div>
          ) : (
            <div className="flex flex-col">
              {recentConversations.map(conv => {
                const idToTrack = conv.conversationId || conv.otherUserId || 0
                const isSent = sendingTo.has(idToTrack)

                return (
                  <div key={idToTrack} className="flex items-center justify-between hover:bg-muted px-4 py-2 transition-colors">
                    <div className="flex items-center gap-3">
                      <Avatar className="h-10 w-10 border border-border">
                        <AvatarImage src={conv.otherUserAvatar || undefined} />
                        <AvatarFallback className={conv.isGroup ? "bg-emerald-500/20 text-emerald-600" : "bg-primary/20 text-primary"}>
                          {conv.isGroup ? <Users className="h-5 w-5" /> : conv.otherUserName?.[0]?.toUpperCase()}
                        </AvatarFallback>
                      </Avatar>
                      <span className="font-semibold text-[15px]">{conv.otherUserName}</span>
                    </div>
                    <Button 
                      size="sm"
                      disabled={isSent}
                      onClick={() => handleSend(conv)}
                      className={`rounded-full px-5 font-semibold transition-colors ${
                        isSent 
                          ? 'bg-muted text-muted-foreground hover:bg-muted' 
                          : 'bg-primary/10 text-primary hover:bg-primary/20'
                      }`}
                    >
                      {isSent ? 'Đã gửi' : 'Gửi'}
                    </Button>
                  </div>
                )
              })}
            </div>
          )}
        </ScrollArea>
      </DialogContent>
    </Dialog>
  )
}
