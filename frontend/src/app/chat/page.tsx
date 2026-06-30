'use client'

import { useState, useEffect, useRef, useCallback } from "react"
import { useSession } from "next-auth/react"
import { useQueryClient } from "@tanstack/react-query"
import { Header } from "@/components/layout/Header"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import {
  Send, Search, MoreVertical, MessageSquare,
  ArrowLeft, Loader2, CheckCheck, Users, Ban, Trash2, LogOut, EyeOff
} from "lucide-react"
import {
  DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator
} from "@/components/ui/dropdown-menu"
import {
  getConversations, getMessages, sendMessage, markConversationAsRead,
  getUnreadCount, searchUsers, deleteConversation, blockUser, unblockUser,
  type ConversationDto, type ChatMessageDto
} from "@/lib/chat-api"
import { useChatWebSocket, type TypingEvent, type BlockEvent } from "@/hooks/useChatWebSocket"

function formatRelativeTime(dateStr: string | null): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  if (diffMins < 1) return 'vừa xong'
  if (diffMins < 60) return `${diffMins} phút trước`
  if (diffMins < 24 * 60) return `${Math.floor(diffMins / 60)} giờ trước`
  if (diffMins < 7 * 24 * 60) return `${Math.floor(diffMins / (24 * 60))} ngày trước`
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
}

function ChatPage() {
  const { data: session } = useSession()
  const currentUserId = (session?.user as any)?.userId as number | undefined
  const queryClient = useQueryClient()

  // ── State ──────────────────────────────────────────────────
  const [conversations, setConversations] = useState<ConversationDto[]>([])
  const [selectedConv, setSelectedConv] = useState<ConversationDto | null>(null)
  const [messages, setMessages] = useState<ChatMessageDto[]>([])
  const [messageInput, setMessageInput] = useState("")
  const [searchQuery, setSearchQuery] = useState("")
  const [searchResults, setSearchResults] = useState<ConversationDto[]>([])
  const [isSearching, setIsSearching] = useState(false)
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [sendingMessage, setSendingMessage] = useState(false)
  const [totalUnread, setTotalUnread] = useState(0)
  const [typingUsers, setTypingUsers] = useState<Map<number, string>>(new Map())
  const [showMobileChat, setShowMobileChat] = useState(false)

  const messagesEndRef = useRef<HTMLDivElement>(null)
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout>>()

  // ── WebSocket ──────────────────────────────────────────────
  const handleWsMessage = useCallback((msg: ChatMessageDto) => {
    if (selectedConv && msg.conversationId === selectedConv.conversationId) {
      setMessages(prev => {
        if (prev.some(m => m.messageId === msg.messageId)) return prev
        return [...prev, msg]
      })
      if (msg.senderId !== currentUserId && selectedConv.conversationId) {
        markConversationAsRead(selectedConv.conversationId).catch(() => { })
      }
    }
    // Update conversation list
    setConversations(prev => {
      const updated = prev.map(c => {
        if (c.conversationId === msg.conversationId) {
          return {
            ...c, lastMessagePreview: msg.content, lastMessageAt: msg.sentAt,
            unreadCount: (selectedConv?.conversationId === msg.conversationId && msg.senderId !== currentUserId) ? 0 : c.unreadCount + (msg.senderId !== currentUserId ? 1 : 0)
          }
        }
        return c
      })
      return updated.sort((a, b) => new Date(b.lastMessageAt || 0).getTime() - new Date(a.lastMessageAt || 0).getTime())
    })
    refreshUnread()
  }, [selectedConv, currentUserId])

  const handleTyping = useCallback((event: TypingEvent) => {
    setTypingUsers(prev => {
      const next = new Map(prev)
      if (event.isTyping) next.set(event.userId, event.userName)
      else next.delete(event.userId)
      return next
    })
  }, [])

  const handleBlockStatus = useCallback((event: BlockEvent) => {
    setConversations(prev => prev.map(c => {
      if (c.otherUserId === event.blockedBy) {
        if (selectedConv && selectedConv.otherUserId === event.blockedBy) {
          setSelectedConv(prevSel => prevSel ? { ...prevSel, blockedByThem: event.blocked } : prevSel)
        }
        return { ...c, blockedByThem: event.blocked }
      }
      return c
    }))
  }, [selectedConv])

  const { sendTypingIndicator } = useChatWebSocket({
    userId: currentUserId ?? null,
    onMessage: handleWsMessage,
    onTyping: handleTyping,
    onBlockStatus: handleBlockStatus
  })

  useEffect(() => {
    const handleMessageRead = (e: any) => {
      const detail = e.detail;
      if (selectedConv && detail.conversationId === selectedConv.conversationId) {
        setMessages(prev => prev.map(m => ({ ...m, isRead: true })))
      }
    }
    window.addEventListener('message_read', handleMessageRead)
    return () => window.removeEventListener('message_read', handleMessageRead)
  }, [selectedConv])

  // ── Data Loading ───────────────────────────────────────────
  const refreshUnread = useCallback(async () => {
    try {
      const r = await getUnreadCount();
      setTotalUnread(r.unreadCount);
      queryClient.setQueryData(['chat', 'unread-count'], r);
    } catch { }
  }, [queryClient])

  useEffect(() => {
    if (!currentUserId) return
    loadConversations()
    refreshUnread()
  }, [currentUserId])

  async function loadConversations() {
    try { const data = await getConversations(); setConversations(data) } catch { }
  }

  async function loadMessages(convId: number) {
    setLoadingMessages(true)
    try {
      const data = await getMessages(convId)
      setMessages([...data.content].reverse())
      await markConversationAsRead(convId)
      refreshUnread()
      setConversations(prev => prev.map(c => c.conversationId === convId ? { ...c, unreadCount: 0 } : c))
    } catch { } finally { setLoadingMessages(false) }
  }

  function selectConversation(conv: ConversationDto) {
    setSelectedConv(conv)
    setShowMobileChat(true)

    // Clear search and add to list if not present
    if (searchQuery) {
      setSearchQuery('')
      setConversations(prev => {
        const exists = prev.find(c =>
          (c.conversationId && c.conversationId === conv.conversationId) ||
          (c.otherUserId && c.otherUserId === conv.otherUserId)
        )
        if (!exists) return [conv, ...prev]
        return prev
      })
    }

    if (conv.conversationId) loadMessages(conv.conversationId)
    else setMessages([])
  }

  // ── Search ─────────────────────────────────────────────────
  useEffect(() => {
    if (searchQuery.length < 2) { setSearchResults([]); return }
    const timer = setTimeout(async () => {
      setIsSearching(true)
      try { const r = await searchUsers(searchQuery); setSearchResults(r) } catch { } finally { setIsSearching(false) }
    }, 300)
    return () => clearTimeout(timer)
  }, [searchQuery])

  // ── Send Message ───────────────────────────────────────────
  async function handleSendMessage() {
    if (!messageInput.trim() || !selectedConv || !currentUserId) return
    setSendingMessage(true)
    try {
      const payload: any = { content: messageInput.trim() }
      if (selectedConv.isGroup || !selectedConv.otherUserId) {
        payload.conversationId = selectedConv.conversationId
      } else {
        payload.recipientId = selectedConv.otherUserId
        if (selectedConv.conversationId) payload.conversationId = selectedConv.conversationId
      }

      const msg = await sendMessage(payload)
      setMessages(prev => {
        if (prev.some(m => m.messageId === msg.messageId)) return prev
        return [...prev, msg]
      })
      setMessageInput("")
      if (!selectedConv.conversationId) {
        setSelectedConv(prev => prev ? { ...prev, conversationId: msg.conversationId } : prev)
        loadConversations()
      }
      if (selectedConv.otherUserId) {
        sendTypingIndicator(selectedConv.otherUserId, false)
      }
    } catch (error: any) { 
      const errorMsg = error?.message || "Lỗi không xác định";
      if (errorMsg.includes("BLOCKED") || error.status === 400 || error.status === 403) {
        alert("Không thể gửi tin nhắn: Bạn hoặc người dùng này đang bị chặn.");
        // Revert UI to blocked state to allow them to unblock properly
        setConversations(prev => prev.map(c => 
          c.otherUserId === selectedConv.otherUserId ? { ...c, blocked: true } : c
        ));
        setSelectedConv(prev => prev ? { ...prev, blocked: true } : prev);
      } else {
        alert("Lỗi khi gửi tin nhắn: " + errorMsg);
      }
    } finally { 
      setSendingMessage(false) 
    }
  }

  // ── Typing Indicator ──────────────────────────────────────
  function handleInputChange(value: string) {
    setMessageInput(value)
    if (selectedConv && selectedConv.otherUserId) {
      sendTypingIndicator(selectedConv.otherUserId, true)
      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current)
      typingTimeoutRef.current = setTimeout(() => {
        if (selectedConv && selectedConv.otherUserId) sendTypingIndicator(selectedConv.otherUserId, false)
      }, 2000)
    }
  }

  // ── Auto-scroll ────────────────────────────────────────────
  useEffect(() => {
    if (!loadingMessages) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages, loadingMessages, selectedConv])

  if (!session) {
    return (
      <div className="h-screen flex flex-col bg-background">
        <Header />
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center space-y-4">
            <MessageSquare className="h-16 w-16 mx-auto text-muted-foreground" />
            <h2 className="text-xl font-semibold">Đăng nhập để sử dụng Chat</h2>
            <p className="text-muted-foreground">Bạn cần đăng nhập để nhắn tin và sử dụng trợ lý AI.</p>
          </div>
        </div>
      </div>
    )
  }

  const isTypingForSelected = selectedConv && typingUsers.has(selectedConv.otherUserId)
  const displayedList = searchQuery.length >= 2 ? searchResults : conversations

  return (
    <div className="h-[100dvh] flex flex-col bg-background overflow-hidden">
      <div className="shrink-0">
        <Header />
      </div>
      <div className="flex-1 min-h-0 container mx-auto px-2 sm:px-4 py-2 sm:py-4 overflow-hidden">
        <div className="h-full flex gap-3 min-h-0">

          {/* ═══ LEFT SIDEBAR ═══ */}
          <div className={`${showMobileChat ? 'hidden md:flex' : 'flex'} w-full md:w-[340px] lg:w-[380px] flex-col bg-card border rounded-xl overflow-hidden flex-shrink-0`}>
            <div className="flex items-center justify-between px-4 py-4">
              <h2 className="text-lg font-bold flex items-center gap-2">
                Đoạn chat
              </h2>
            </div>

            {/* Search */}
            <div className="px-3 pb-3">
              <div className="relative">
                <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input placeholder="Tìm người dùng..." className="pl-9 h-10 bg-muted/50 border-transparent focus-visible:ring-1 rounded-full" value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} />
              </div>
            </div>
            <Separator />
            {/* Conversation List */}
            <ScrollArea className="flex-1">
              {isSearching && <div className="flex justify-center py-8"><Loader2 className="h-5 w-5 animate-spin text-muted-foreground" /></div>}
              {!isSearching && displayedList.length === 0 && (
                <div className="text-center py-12 px-4">
                  <MessageSquare className="h-10 w-10 mx-auto text-muted-foreground/50 mb-3" />
                  <p className="text-sm text-muted-foreground">{searchQuery ? 'Không tìm thấy người dùng' : 'Chưa có cuộc trò chuyện nào'}</p>
                </div>
              )}
              {displayedList.map((conv, i) => (
                <button key={(conv.otherUserId || conv.conversationId) + '-' + i} onClick={() => selectConversation(conv)}
                  className={`w-full p-3.5 flex gap-3 hover:bg-muted/60 transition-all duration-150 ${selectedConv?.conversationId === conv.conversationId ? 'bg-primary/5 border-l-2 border-l-primary' : 'border-l-2 border-l-transparent'}`}>
                  <div className="relative flex-shrink-0">
                    <Avatar className="h-12 w-12">
                      <AvatarImage src={conv.otherUserAvatar || undefined} />
                      <AvatarFallback className={conv.isGroup ? "bg-emerald-100 text-emerald-600" : "bg-primary/10 text-primary font-semibold text-sm"}>
                        {conv.isGroup ? <Users className="h-5 w-5" /> : (conv.otherUserName?.[0]?.toUpperCase() || '?')}
                      </AvatarFallback>
                    </Avatar>
                    {conv.otherUserOnline && <div className="absolute bottom-0 right-0 w-3.5 h-3.5 bg-green-500 rounded-full border-2 border-card" />}
                  </div>
                  <div className="flex-1 text-left min-w-0 flex flex-col justify-center">
                    <div className="flex items-center justify-between mb-1">
                      <h4 className="text-sm font-semibold truncate">{conv.otherUserName}</h4>
                      {conv.lastMessageAt && <span className="text-[11px] text-muted-foreground flex-shrink-0 ml-2">{formatRelativeTime(conv.lastMessageAt)}</span>}
                    </div>
                    <div className="flex items-center justify-between">
                      <p className={`text-xs truncate pr-2 ${conv.unreadCount > 0 ? 'text-foreground font-bold' : 'text-muted-foreground'}`}>{conv.lastMessagePreview || 'Bắt đầu trò chuyện...'}</p>
                      {conv.unreadCount > 0 && <span className="bg-primary text-primary-foreground text-[10px] font-bold px-1.5 py-0.5 rounded-full flex-shrink-0">{conv.unreadCount}</span>}
                    </div>
                  </div>
                </button>
              ))}
            </ScrollArea>
          </div>

          {/* ═══ RIGHT: CHAT WINDOW ═══ */}
          <div className={`${showMobileChat ? 'flex' : 'hidden md:flex'} flex-1 min-h-0 flex-col bg-card border rounded-xl overflow-hidden`}>
            {selectedConv ? (
              <>
                {/* Chat Header */}
                <div className="shrink-0 px-4 py-3 border-b flex items-center justify-between bg-card/80 backdrop-blur-sm">
                  <div className="flex items-center gap-3">
                    <Button variant="ghost" size="sm" className="md:hidden h-8 w-8 p-0" onClick={() => setShowMobileChat(false)}>
                      <ArrowLeft className="h-5 w-5" />
                    </Button>
                    <div className="relative">
                      <Avatar className="h-10 w-10">
                        <AvatarImage src={selectedConv.otherUserAvatar || undefined} />
                        <AvatarFallback className={selectedConv.isGroup ? "bg-emerald-100 text-emerald-600" : "bg-primary/10 text-primary font-semibold"}>
                          {selectedConv.isGroup ? <Users className="h-5 w-5" /> : selectedConv.otherUserName?.[0]?.toUpperCase()}
                        </AvatarFallback>
                      </Avatar>
                      {selectedConv.otherUserOnline && <div className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-card" />}
                    </div>
                    <div>
                      <h3 className="text-sm font-semibold">{selectedConv.otherUserName}</h3>
                      <p className="text-xs text-muted-foreground">
                        {isTypingForSelected ? <span className="text-primary animate-pulse">Đang nhập...</span> : selectedConv.isGroup ? 'Nhóm trò chuyện' : selectedConv.otherUserOnline ? 'Đang hoạt động' : ''}
                      </p>
                    </div>
                  </div>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                        <MoreVertical className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="w-56">
                      <DropdownMenuItem className="cursor-pointer" onClick={() => {
                        // Mark as unread placeholder
                        if (selectedConv?.conversationId) {
                          setConversations(prev => prev.map(c =>
                            c.conversationId === selectedConv.conversationId ? { ...c, unreadCount: 1 } : c
                          ))
                        }
                      }}>
                        <EyeOff className="h-4 w-4 mr-2" /> Đánh dấu chưa đọc
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      {!selectedConv?.isGroup && (
                        selectedConv?.blocked ? (
                          <DropdownMenuItem className="cursor-pointer text-primary focus:text-primary" onClick={async () => {
                            if (selectedConv?.otherUserId) {
                              if (!window.confirm(`Bạn có chắc chắn muốn bỏ chặn ${selectedConv.otherUserName || 'người dùng này'}?`)) return;
                              const targetUserId = selectedConv.otherUserId;
                              // Optimistic update
                              setConversations(prev => prev.map(c =>
                                c.otherUserId === targetUserId ? { ...c, blocked: false } : c
                              ))
                              setSelectedConv(prev => prev ? { ...prev, blocked: false } : prev)

                              try {
                                await unblockUser(targetUserId)
                              } catch (err) {
                                // Revert on failure
                                setConversations(prev => prev.map(c =>
                                  c.otherUserId === targetUserId ? { ...c, blocked: true } : c
                                ))
                                setSelectedConv(prev => prev ? { ...prev, blocked: true } : prev)
                                alert("Không thể bỏ chặn người dùng này. Vui lòng thử lại sau.")
                              }
                            }
                          }}>
                            <Ban className="h-4 w-4 mr-2" /> Bỏ chặn người dùng
                          </DropdownMenuItem>
                        ) : (
                          <DropdownMenuItem className="cursor-pointer text-destructive focus:text-destructive" onClick={async () => {
                            if (selectedConv?.otherUserId) {
                              if (!window.confirm(`Bạn có chắc chắn muốn chặn ${selectedConv.otherUserName || 'người dùng này'}?`)) return;
                              const targetUserId = selectedConv.otherUserId;
                              // Optimistic update
                              setConversations(prev => prev.map(c =>
                                c.otherUserId === targetUserId ? { ...c, blocked: true } : c
                              ))
                              setSelectedConv(prev => prev ? { ...prev, blocked: true } : prev)

                              try {
                                await blockUser(targetUserId)
                              } catch (err) {
                                // Revert on failure
                                setConversations(prev => prev.map(c =>
                                  c.otherUserId === targetUserId ? { ...c, blocked: false } : c
                                ))
                                setSelectedConv(prev => prev ? { ...prev, blocked: false } : prev)
                                alert("Không thể chặn người dùng này. Vui lòng thử lại sau.")
                              }
                            }
                          }}>
                            <Ban className="h-4 w-4 mr-2" /> Chặn người dùng
                          </DropdownMenuItem>
                        )
                      )}
                      <DropdownMenuItem className="cursor-pointer text-destructive focus:text-destructive" onClick={async () => {
                        if (selectedConv?.conversationId) {
                          try {
                            await deleteConversation(selectedConv.conversationId)
                            setConversations(prev => prev.filter(c => c.conversationId !== selectedConv.conversationId))
                            setSelectedConv(null)
                          } catch { }
                        }
                      }}>
                        <Trash2 className="h-4 w-4 mr-2" /> Xóa đoạn chat
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>

                {/* Messages Area */}
                <ScrollArea className="flex-1 min-h-0 p-4">
                  {loadingMessages ? (
                    <div className="flex justify-center py-12"><Loader2 className="h-6 w-6 animate-spin text-muted-foreground" /></div>
                  ) : messages.length === 0 ? (
                    <div className="text-center py-16">
                      <MessageSquare className="h-12 w-12 mx-auto text-muted-foreground/30 mb-3" />
                      <p className="text-sm text-muted-foreground">Bắt đầu cuộc trò chuyện!</p>
                    </div>
                  ) : (
                    <div className="space-y-2">
                      {messages.map((msg, index) => {
                        const isMe = msg.senderId === currentUserId
                        const isLast = index === messages.length - 1
                        const showStatus = isLast

                        return (
                          <div key={msg.messageId} className={`flex ${isMe ? 'justify-end' : 'justify-start'} animate-fade-in-up`}>
                            <div className={`flex flex-col ${isMe ? 'items-end' : 'items-start'} max-w-[75%] sm:max-w-sm md:max-w-md`}>
                              <div className={`${isMe ? 'bg-primary text-primary-foreground rounded-2xl rounded-br-md' : 'bg-muted rounded-2xl rounded-bl-md'} px-3.5 py-2`}>
                                {selectedConv.isGroup && !isMe && (
                                  <p className="text-xs font-semibold text-primary mb-1">{msg.senderName}</p>
                                )}
                                <p className="text-sm whitespace-pre-wrap break-words">{msg.content}</p>
                              </div>
                              {showStatus && (
                                <div className={`flex items-center gap-1 mt-1 ${isMe ? 'justify-end' : 'justify-start'}`}>
                                  <span className="text-[10px] text-muted-foreground">
                                    {isMe
                                      ? (msg.isRead ? 'Đã xem' : `Đã gửi ${formatRelativeTime(msg.sentAt)}`)
                                      : formatRelativeTime(msg.sentAt)}
                                  </span>
                                  {isMe && msg.isRead && !selectedConv.isGroup && (
                                    <div className="w-3.5 h-3.5 rounded-full overflow-hidden border border-background ml-1 shrink-0">
                                      {selectedConv.otherUserAvatar ? (
                                        <img src={selectedConv.otherUserAvatar} className="w-full h-full object-cover" alt="Seen" />
                                      ) : (
                                        <div className="w-full h-full bg-emerald-100 flex items-center justify-center text-[7px] text-emerald-600 font-bold">{selectedConv.otherUserName?.[0]?.toUpperCase()}</div>
                                      )}
                                    </div>
                                  )}
                                  {isMe && !msg.isRead && !selectedConv.isGroup && (
                                    <div className="w-2.5 h-2.5 rounded-full bg-muted-foreground/40 ml-1 shrink-0" />
                                  )}
                                </div>
                              )}
                            </div>
                          </div>
                        )
                      })}
                      {isTypingForSelected && (
                        <div className="flex justify-start">
                          <div className="bg-muted rounded-2xl rounded-bl-md px-4 py-2.5">
                            <div className="flex gap-1.5">
                              <div className="w-2 h-2 bg-muted-foreground/60 rounded-full animate-bounce" />
                              <div className="w-2 h-2 bg-muted-foreground/60 rounded-full animate-bounce" style={{ animationDelay: '0.1s' }} />
                              <div className="w-2 h-2 bg-muted-foreground/60 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }} />
                            </div>
                          </div>
                        </div>
                      )}
                      <div ref={messagesEndRef} />
                    </div>
                  )}
                </ScrollArea>

                {/* Message Input */}
                <div className="shrink-0 px-4 py-3 border-t bg-card/80 backdrop-blur-sm">
                  {selectedConv.blocked ? (
                    <div className="text-center py-2 text-sm text-destructive font-medium bg-destructive/10 rounded-lg">Bạn đã chặn người dùng này</div>
                  ) : selectedConv.blockedByThem ? (
                    <div className="text-center py-2 text-sm text-muted-foreground bg-muted rounded-lg">Bạn không thể trả lời cuộc trò chuyện này</div>
                  ) : (
                    <div className="flex gap-2 items-end">
                      <Input placeholder="Nhập tin nhắn..." value={messageInput}
                        onChange={(e) => handleInputChange(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey && messageInput.trim()) { e.preventDefault(); handleSendMessage() } }}
                        className="h-10" disabled={sendingMessage} />
                      <Button className="h-10 px-4" disabled={!messageInput.trim() || sendingMessage} onClick={handleSendMessage}>
                        {sendingMessage ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
                      </Button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              /* Empty state */
              <div className="flex-1 flex items-center justify-center">
                <div className="text-center space-y-4 p-8">
                  <div className="w-20 h-20 mx-auto rounded-full bg-gradient-to-br from-primary/20 to-emerald-500/20 flex items-center justify-center">
                    <MessageSquare className="h-10 w-10 text-primary" />
                  </div>
                  <h2 className="text-lg font-semibold">Chọn một cuộc trò chuyện</h2>
                  <p className="text-sm text-muted-foreground max-w-sm">Chọn một người bạn từ danh sách bên trái hoặc tìm kiếm để bắt đầu trò chuyện mới.</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default ChatPage
