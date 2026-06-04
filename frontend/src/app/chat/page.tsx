'use client'

import { useState, useRef, useEffect } from "react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { 
  Send, 
  Smile, 
  Paperclip, 
  Search, 
  MoreVertical, 
  Phone, 
  Video, 
  Info,
  Check,
  CheckCheck
} from "lucide-react";
import { toast } from "sonner";

interface Message {
  id: number;
  sender: "me" | "other";
  content: string;
  timestamp: string;
  status?: "sent" | "delivered" | "read";
}

const initialConversations = [
  {
    id: 1,
    name: "Nguyễn Văn A",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=1",
    lastMessage: "Ok, hẹn gặp bạn tối nay!",
    timestamp: "14:30",
    unread: 2,
    online: true,
    role: "Khách hàng"
  },
  {
    id: 2,
    name: "Sân bóng Thành Công",
    avatar: "https://images.unsplash.com/photo-1544698310-74ea9d1c8258?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=120",
    lastMessage: "Đơn của bạn đã được xác nhận",
    timestamp: "10:15",
    unread: 0,
    online: false,
    role: "Chủ sân"
  },
  {
    id: 3,
    name: "Trần Thị B",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=2",
    lastMessage: "Bạn có rảnh chiều mai không?",
    timestamp: "Hôm qua",
    unread: 0,
    online: true,
    role: "Khách hàng"
  },
];

const initialMessages: Record<number, Message[]> = {
  1: [
    {
      id: 1,
      sender: "other",
      content: "Chào bạn! Tôi thấy bạn đăng tìm người chơi bóng ở Arena Sports Center.",
      timestamp: "14:20",
    },
    {
      id: 2,
      sender: "me",
      content: "Chào bạn! Đúng rồi, bên mình đang thiếu 2 người đá cánh. Bạn tham gia được không?",
      timestamp: "14:22",
      status: "read",
    },
    {
      id: 3,
      sender: "other",
      content: "Được chứ! Tối nay lúc 18h phải không? Mình có thể rủ thêm 1 người nữa đi cùng.",
      timestamp: "14:25",
    },
    {
      id: 4,
      sender: "me",
      content: "Đúng rồi! Sân Arena Sports Center, Quận 3 nhé. Hẹn gặp mọi người lúc 18h.",
      timestamp: "14:27",
      status: "read",
    },
    {
      id: 5,
      sender: "other",
      content: "Ok, hẹn gặp bạn tối nay!",
      timestamp: "14:30",
    },
  ],
  2: [
    {
      id: 1,
      sender: "other",
      content: "Chào bạn! Sân bóng Thành Công xin chào. Chúng tôi đã tiếp nhận yêu cầu đặt sân của bạn.",
      timestamp: "09:00",
    },
    {
      id: 2,
      sender: "me",
      content: "Sân mình tối nay có hỗ trợ mượn áo pitch không ạ?",
      timestamp: "09:30",
      status: "read",
    },
    {
      id: 3,
      sender: "other",
      content: "Dạ có ạ, bên em chuẩn bị sẵn áo pitch miễn phí cho khách hàng đặt sân 2h ạ.",
      timestamp: "09:45",
    },
    {
      id: 4,
      sender: "other",
      content: "Đơn đặt sân mã BK001234 của bạn đã được xác nhận thành công.",
      timestamp: "10:15",
    },
  ],
  3: [
    {
      id: 1,
      sender: "other",
      content: "Chào bạn, sân bóng Phú Mỹ Hưng chiều mai còn trống khung giờ nào không nhỉ?",
      timestamp: "Hôm qua",
    },
    {
      id: 2,
      sender: "me",
      content: "Chiều mai khoảng từ 16h - 18h bên mình còn 1 sân cỏ 7 người trống bạn nha.",
      timestamp: "Hôm qua",
      status: "read",
    },
    {
      id: 3,
      sender: "other",
      content: "Bạn có rảnh chiều mai không?",
      timestamp: "Hôm qua",
    },
  ],
};

const autoReplies: Record<number, string[]> = {
  1: [
    "Mình vừa chuẩn bị đồ xong, hẹn tí nữa gặp trên sân nhé!",
    "Bên bạn mặc áo màu gì thế để mình chuẩn bị áo cùng màu?",
    "Ok nha, chiến hết mình tối nay thôi!"
  ],
  2: [
    "Cảm ơn quý khách đã tin tưởng dịch vụ của Sân bóng Thành Công. Chúc bạn có những giờ phút chơi bóng vui vẻ!",
    "Nếu cần hỗ trợ thêm thông tin gì khác, quý khách vui lòng nhắn tin trực tiếp tại đây nhé.",
    "Bên em có bán các loại nước giải khát và đồ ăn nhẹ ngay tại quầy lễ tân ạ."
  ],
  3: [
    "Ok cậu, có gì chiều mai mình qua đúng giờ nha.",
    "Cảm ơn thông tin của bạn nhiều nhé!",
    "Có gì mình rủ thêm bạn rồi nhắn lại cậu sau nhé."
  ]
};

function ChatPage() {
  const [messageText, setMessageText] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedChat, setSelectedChat] = useState(1);
  const [conversations, setConversations] = useState(initialConversations);
  const [allMessages, setAllMessages] = useState<Record<number, Message[]>>(initialMessages);
  const [isTyping, setIsTyping] = useState(false);
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom of chat
  useEffect(() => {
    if (scrollContainerRef.current) {
      scrollContainerRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [allMessages, selectedChat, isTyping]);

  const selectedConversation = conversations.find((c) => c.id === selectedChat);
  const currentMessages = allMessages[selectedChat] || [];

  // Filter conversations list dynamically by search query
  const filteredConversations = conversations.filter(conv => 
    conv.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    conv.lastMessage.toLowerCase().includes(searchQuery.toLowerCase()) ||
    conv.role.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleSendMessage = () => {
    if (!messageText.trim()) return;

    const newMessage: Message = {
      id: Date.now(),
      sender: "me",
      content: messageText,
      timestamp: new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" }),
      status: "sent"
    };

    // Append to current list of messages
    setAllMessages(prev => ({
      ...prev,
      [selectedChat]: [...(prev[selectedChat] || []), newMessage]
    }));

    // Update last message in sidebar
    setConversations(prev => prev.map(conv => {
      if (conv.id === selectedChat) {
        return {
          ...conv,
          lastMessage: messageText,
          timestamp: "Vừa xong",
          unread: 0
        };
      }
      return conv;
    }));

    setMessageText("");

    // Simulate double ticks (delivered -> read)
    setTimeout(() => {
      setAllMessages(prev => {
        const chatMsgs = prev[selectedChat] || [];
        return {
          ...prev,
          [selectedChat]: chatMsgs.map(m => m.id === newMessage.id ? { ...m, status: "read" } : m)
        };
      });
    }, 1500);

    // Simulate auto-reply response after 2.5 seconds
    setTimeout(() => {
      setIsTyping(true);
    }, 2000);

    setTimeout(() => {
      setIsTyping(false);
      const replies = autoReplies[selectedChat] || ["Cảm ơn tin nhắn của bạn!"];
      const randomReply = replies[Math.floor(Math.random() * replies.length)];
      
      const replyMessage: Message = {
        id: Date.now() + 1,
        sender: "other",
        content: randomReply,
        timestamp: new Date().toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })
      };

      setAllMessages(prev => ({
        ...prev,
        [selectedChat]: [...(prev[selectedChat] || []), replyMessage]
      }));

      setConversations(prev => prev.map(conv => {
        if (conv.id === selectedChat) {
          return {
            ...conv,
            lastMessage: randomReply,
            timestamp: "Vừa xong"
          };
        }
        return conv;
      }));
    }, 4500);
  };

  const handleClearUnread = (chatId: number) => {
    setSelectedChat(chatId);
    setConversations(prev => prev.map(conv => {
      if (conv.id === chatId) {
        return { ...conv, unread: 0 };
      }
      return conv;
    }));
  };

  return (
    <div className="h-screen flex flex-col bg-neutral-50/50 dark:bg-zinc-950 font-sans">
      <Header />

      <div className="flex-1 container mx-auto px-4 py-6 overflow-hidden max-w-6xl">
        <div className="h-full grid grid-cols-1 md:grid-cols-3 gap-6">
          
          {/* Sidebar danh sách tin nhắn */}
          <div className="md:col-span-1 bg-white dark:bg-zinc-900 border border-neutral-100 dark:border-zinc-800 rounded-2xl flex flex-col shadow-sm overflow-hidden">
            <div className="p-4 space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="text-xl font-bold text-neutral-800 dark:text-neutral-100">Tin nhắn</h2>
                <Badge variant="secondary" className="bg-emerald-500/10 text-emerald-600 border border-emerald-500/20 px-2.5 py-0.5 rounded-full font-semibold">
                  Mới
                </Badge>
              </div>
              <div className="relative">
                <Search className="absolute left-3 top-2.5 h-4.5 w-4.5 text-neutral-400" />
                <Input 
                  placeholder="Tìm hội thoại, sân bóng..." 
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10 h-10 rounded-xl bg-neutral-50 dark:bg-zinc-800 border-neutral-100 dark:border-zinc-800 focus-visible:ring-emerald-500" 
                />
              </div>
            </div>

            <Separator className="bg-neutral-100/70 dark:bg-zinc-800/70" />

            <ScrollArea className="flex-1 py-2">
              <div className="space-y-1 px-2">
                {filteredConversations.length === 0 ? (
                  <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
                    <Search className="h-8 w-8 text-neutral-300 dark:text-zinc-700 mb-2 animate-pulse" />
                    <p className="text-sm font-semibold text-neutral-700 dark:text-neutral-300">Không tìm thấy hội thoại</p>
                    <p className="text-xs text-neutral-400 mt-1">Vui lòng thử từ khóa tìm kiếm khác</p>
                  </div>
                ) : (
                  filteredConversations.map((conv) => {
                    const isActive = selectedChat === conv.id;
                    return (
                      <button
                        key={conv.id}
                        onClick={() => handleClearUnread(conv.id)}
                        className={`w-full p-3.5 flex gap-3.5 rounded-2xl transition-all ${
                          isActive 
                            ? "bg-emerald-500/5 text-neutral-800 border-l-4 border-emerald-500 font-medium" 
                            : "hover:bg-neutral-50 dark:hover:bg-zinc-800/50 text-neutral-600"
                        }`}
                      >
                        <div className="relative">
                          <Avatar className="h-11 w-11 ring-2 ring-neutral-100 dark:ring-zinc-800">
                            <AvatarImage src={conv.avatar} className="object-cover" />
                            <AvatarFallback className="bg-emerald-100 text-emerald-600 font-bold">
                              {conv.name[0]}
                            </AvatarFallback>
                          </Avatar>
                          {conv.online && (
                            <div className="absolute bottom-0 right-0 w-3 h-3 bg-emerald-500 rounded-full border-2 border-white dark:border-zinc-900 animate-pulse" />
                          )}
                        </div>

                        <div className="flex-1 text-left min-w-0">
                          <div className="flex items-center justify-between mb-1">
                            <h4 className="text-sm font-bold text-neutral-800 dark:text-neutral-100 truncate">{conv.name}</h4>
                            <span className="text-xs text-neutral-400 flex-shrink-0">
                              {conv.timestamp}
                            </span>
                          </div>
                          <div className="flex items-center justify-between">
                            <p className={`text-xs truncate ${conv.unread > 0 ? 'text-neutral-800 dark:text-neutral-100 font-semibold' : 'text-neutral-400'}`}>
                              {conv.lastMessage}
                            </p>
                            {conv.unread > 0 && (
                              <Badge className="ml-2 bg-emerald-500 text-white hover:bg-emerald-600 text-xxs px-1.5 py-0 h-4.5 rounded-full flex items-center justify-center font-bold">
                                {conv.unread}
                              </Badge>
                            )}
                          </div>
                        </div>
                      </button>
                    );
                  })
                )}
              </div>
            </ScrollArea>
          </div>

          {/* Cửa sổ chat chính */}
          <div className="md:col-span-2 bg-white dark:bg-zinc-900 border border-neutral-100 dark:border-zinc-800 rounded-2xl flex flex-col shadow-sm overflow-hidden">
            
            {/* Header phòng chat */}
            <div className="p-4 border-b border-neutral-100 dark:border-zinc-800 flex items-center justify-between bg-neutral-50/30 dark:bg-zinc-900/30">
              <div className="flex items-center gap-3.5">
                <div className="relative">
                  <Avatar className="h-11 w-11 ring-2 ring-neutral-100 dark:ring-zinc-800">
                    <AvatarImage src={selectedConversation?.avatar} className="object-cover" />
                    <AvatarFallback className="bg-emerald-100 text-emerald-600 font-bold">
                      {selectedConversation?.name[0]}
                    </AvatarFallback>
                  </Avatar>
                  {selectedConversation?.online && (
                    <div className="absolute bottom-0 right-0 w-3 h-3 bg-emerald-500 rounded-full border-2 border-white dark:border-zinc-900" />
                  )}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-sm font-bold text-neutral-800 dark:text-neutral-100">{selectedConversation?.name}</h3>
                    <Badge variant="outline" className="text-xxs px-1.5 py-0 rounded bg-neutral-100 border-neutral-200 text-neutral-500 dark:bg-zinc-800 dark:border-zinc-700">
                      {selectedConversation?.role}
                    </Badge>
                  </div>
                  <p className="text-xs text-neutral-400 flex items-center gap-1.5">
                    <span className={`w-1.5 h-1.5 rounded-full ${selectedConversation?.online ? 'bg-emerald-500' : 'bg-neutral-300'}`} />
                    {selectedConversation?.online ? "Đang trực tuyến" : "Ngoại tuyến"}
                  </p>
                </div>
              </div>
              
              <div className="flex items-center gap-1">
                <Button variant="ghost" size="icon" className="h-9 w-9 rounded-full text-neutral-500 hover:bg-neutral-100 dark:hover:bg-zinc-800" onClick={() => toast.info("Đang kết nối cuộc gọi thoại...")}>
                  <Phone className="h-4.5 w-4.5" />
                </Button>
                <Button variant="ghost" size="icon" className="h-9 w-9 rounded-full text-neutral-500 hover:bg-neutral-100 dark:hover:bg-zinc-800" onClick={() => toast.info("Đang kết nối cuộc gọi video...")}>
                  <Video className="h-4.5 w-4.5" />
                </Button>
                <Button variant="ghost" size="icon" className="h-9 w-9 rounded-full text-neutral-500 hover:bg-neutral-100 dark:hover:bg-zinc-800">
                  <MoreVertical className="h-4.5 w-4.5" />
                </Button>
              </div>
            </div>

            {/* Khung chat tin nhắn */}
            <ScrollArea className="flex-1 p-4 bg-neutral-50/20 dark:bg-zinc-950/20">
              <div className="space-y-4">
                {currentMessages.map((msg) => {
                  const isMe = msg.sender === "me";
                  return (
                    <div
                      key={msg.id}
                      className={`flex ${isMe ? "justify-end" : "justify-start"} animate-fade-in`}
                    >
                      <div className="flex flex-col space-y-1 max-w-[70%]">
                        <div
                          className={`px-4 py-2.5 shadow-sm text-sm ${
                            isMe
                              ? "bg-gradient-to-tr from-emerald-500 to-teal-500 text-white rounded-2xl rounded-tr-sm"
                              : "bg-white dark:bg-zinc-800 border border-neutral-100 dark:border-zinc-800 text-neutral-800 dark:text-neutral-200 rounded-2xl rounded-tl-sm"
                          }`}
                        >
                          <p className="leading-relaxed whitespace-pre-wrap">{msg.content}</p>
                        </div>
                        <div className={`flex items-center gap-1.5 text-xxs text-neutral-400 ${isMe ? "justify-end" : "justify-start"}`}>
                          <span>{msg.timestamp}</span>
                          {isMe && (
                            msg.status === "read" ? (
                              <CheckCheck className="h-3 w-3 text-emerald-500" />
                            ) : (
                              <Check className="h-3 w-3 text-neutral-400" />
                            )
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}

                {/* Nhãn đang gõ tin nhắn */}
                {isTyping && (
                  <div className="flex justify-start items-center gap-2.5 animate-pulse">
                    <Avatar className="h-7 w-7">
                      <AvatarImage src={selectedConversation?.avatar} />
                      <AvatarFallback>{selectedConversation?.name[0]}</AvatarFallback>
                    </Avatar>
                    <div className="bg-white dark:bg-zinc-800 border border-neutral-100 dark:border-zinc-800 rounded-2xl rounded-tl-sm px-4 py-3 flex items-center justify-center gap-1">
                      <div className="w-1.5 h-1.5 bg-neutral-400 dark:bg-neutral-500 rounded-full animate-bounce [animation-delay:-0.3s]" />
                      <div className="w-1.5 h-1.5 bg-neutral-400 dark:bg-neutral-500 rounded-full animate-bounce [animation-delay:-0.15s]" />
                      <div className="w-1.5 h-1.5 bg-neutral-400 dark:bg-neutral-500 rounded-full animate-bounce" />
                    </div>
                  </div>
                )}
                <div ref={scrollContainerRef} />
              </div>
            </ScrollArea>

            {/* Ô nhập tin nhắn */}
            <div className="p-4 border-t border-neutral-100 dark:border-zinc-800 bg-white dark:bg-zinc-900">
              <div className="flex items-center gap-2 bg-neutral-50 dark:bg-zinc-800/50 p-2.5 rounded-xl border border-neutral-100 dark:border-zinc-800 focus-within:ring-2 focus-within:ring-emerald-500/20 focus-within:border-emerald-500 transition-all">
                <Button variant="ghost" size="icon" className="h-9 w-9 rounded-full text-neutral-500 hover:bg-neutral-100 dark:hover:bg-zinc-800" onClick={() => toast.info("Tính năng gửi emoji sắp ra mắt!")}>
                  <Smile className="h-5 w-5" />
                </Button>
                <Button variant="ghost" size="icon" className="h-9 w-9 rounded-full text-neutral-500 hover:bg-neutral-155 dark:hover:bg-zinc-800" onClick={() => toast.info("Hỗ trợ tải tệp tin và ảnh...")}>
                  <Paperclip className="h-5 w-5" />
                </Button>
                <Input
                  placeholder="Nhập nội dung tin nhắn..."
                  value={messageText}
                  onChange={(e) => setMessageText(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      handleSendMessage();
                    }
                  }}
                  className="flex-1 bg-transparent border-0 focus-visible:ring-0 focus-visible:ring-offset-0 h-9 p-0 text-sm"
                />
                <Button 
                  onClick={handleSendMessage}
                  disabled={!messageText.trim()} 
                  className={`h-9 w-9 p-0 rounded-xl transition-all ${
                    messageText.trim() 
                      ? "bg-emerald-500 hover:bg-emerald-600 text-white" 
                      : "bg-neutral-100 text-neutral-400 dark:bg-zinc-800 dark:text-zinc-600"
                  }`}
                >
                  <Send className="h-4.5 w-4.5" />
                </Button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ChatPage;
