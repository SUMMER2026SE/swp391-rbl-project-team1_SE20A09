'use client'

import { useState } from "react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Send, Smile, Paperclip, Search, MoreVertical } from "lucide-react";

export function ChatPage() {
  const [message, setMessage] = useState("");
  const [selectedChat, setSelectedChat] = useState(1);

  const conversations = [
    {
      id: 1,
      name: "Nguyễn Văn A",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=1",
      lastMessage: "Ok, hẹn gặp bạn tối nay!",
      timestamp: "14:30",
      unread: 2,
      online: true,
    },
    {
      id: 2,
      name: "Sân bóng Thành Công",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=venue1",
      lastMessage: "Đơn của bạn đã được xác nhận",
      timestamp: "10:15",
      unread: 0,
      online: false,
    },
    {
      id: 3,
      name: "Trần Thị B",
      avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=2",
      lastMessage: "Bạn có rảnh chiều mai không?",
      timestamp: "Hôm qua",
      unread: 1,
      online: true,
    },
  ];

  const messages = [
    {
      id: 1,
      sender: "other",
      content: "Chào bạn! Tôi thấy bạn đăng tìm người chơi bóng",
      timestamp: "14:20",
    },
    {
      id: 2,
      sender: "me",
      content: "Chào bạn! Đúng rồi, bạn có thể tham gia không?",
      timestamp: "14:22",
    },
    {
      id: 3,
      sender: "other",
      content: "Được chứ! Tối nay lúc 18h phải không?",
      timestamp: "14:25",
    },
    {
      id: 4,
      sender: "me",
      content: "Đúng rồi! Sân bóng Thành Công, Quận 1 nhé",
      timestamp: "14:27",
    },
    {
      id: 5,
      sender: "other",
      content: "Ok, hẹn gặp bạn tối nay!",
      timestamp: "14:30",
      typing: false,
    },
  ];

  const selectedConversation = conversations.find(
    (c) => c.id === selectedChat
  );

  return (
    <div className="h-screen flex flex-col bg-background">
      <Header />

      <div className="flex-1 container mx-auto px-4 py-4 overflow-hidden">
        <div className="h-full grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Conversations List */}
          <div className="md:col-span-1 bg-card border rounded-lg flex flex-col">
            <div className="p-4">
              <h2 className="mb-4">Tin nhắn</h2>
              <div className="relative">
                <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                <Input placeholder="Tìm kiếm..." className="pl-9" />
              </div>
            </div>

            <Separator />

            <ScrollArea className="flex-1">
              {conversations.map((conv) => (
                <button
                  key={conv.id}
                  onClick={() => setSelectedChat(conv.id)}
                  className={`w-full p-4 flex gap-3 hover:bg-muted transition-colors ${
                    selectedChat === conv.id ? "bg-muted" : ""
                  }`}
                >
                  <div className="relative">
                    <Avatar>
                      <AvatarImage src={conv.avatar} />
                      <AvatarFallback>{conv.name[0]}</AvatarFallback>
                    </Avatar>
                    {conv.online && (
                      <div className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-card" />
                    )}
                  </div>

                  <div className="flex-1 text-left min-w-0">
                    <div className="flex items-center justify-between mb-1">
                      <h4 className="text-sm truncate">{conv.name}</h4>
                      <span className="text-xs text-muted-foreground flex-shrink-0 ml-2">
                        {conv.timestamp}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <p className="text-sm text-muted-foreground truncate">
                        {conv.lastMessage}
                      </p>
                      {conv.unread > 0 && (
                        <Badge className="ml-2 bg-primary text-xs px-2 py-0 h-5 flex-shrink-0">
                          {conv.unread}
                        </Badge>
                      )}
                    </div>
                  </div>
                </button>
              ))}
            </ScrollArea>
          </div>

          {/* Chat Window */}
          <div className="md:col-span-2 bg-card border rounded-lg flex flex-col">
            {/* Chat Header */}
            <div className="p-4 border-b flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="relative">
                  <Avatar>
                    <AvatarImage src={selectedConversation?.avatar} />
                    <AvatarFallback>
                      {selectedConversation?.name[0]}
                    </AvatarFallback>
                  </Avatar>
                  {selectedConversation?.online && (
                    <div className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-card" />
                  )}
                </div>
                <div>
                  <h3 className="text-sm">{selectedConversation?.name}</h3>
                  <p className="text-xs text-muted-foreground">
                    {selectedConversation?.online
                      ? "Đang hoạt động"
                      : "Offline"}
                  </p>
                </div>
              </div>
              <Button variant="ghost" size="sm">
                <MoreVertical className="h-5 w-5" />
              </Button>
            </div>

            {/* Messages */}
            <ScrollArea className="flex-1 p-4">
              <div className="space-y-4">
                {messages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`flex ${
                      msg.sender === "me" ? "justify-end" : "justify-start"
                    }`}
                  >
                    <div
                      className={`max-w-sm md:max-w-md ${
                        msg.sender === "me"
                          ? "bg-primary text-primary-foreground"
                          : "bg-muted"
                      } rounded-lg px-4 py-2`}
                    >
                      <p className="text-sm">{msg.content}</p>
                      <p
                        className={`text-xs mt-1 ${
                          msg.sender === "me"
                            ? "text-primary-foreground/70"
                            : "text-muted-foreground"
                        }`}
                      >
                        {msg.timestamp}
                      </p>
                    </div>
                  </div>
                ))}

                {/* Typing Indicator */}
                <div className="flex justify-start">
                  <div className="bg-muted rounded-lg px-4 py-2">
                    <div className="flex gap-1">
                      <div className="w-2 h-2 bg-muted-foreground rounded-full animate-bounce" />
                      <div
                        className="w-2 h-2 bg-muted-foreground rounded-full animate-bounce"
                        style={{ animationDelay: "0.1s" }}
                      />
                      <div
                        className="w-2 h-2 bg-muted-foreground rounded-full animate-bounce"
                        style={{ animationDelay: "0.2s" }}
                      />
                    </div>
                  </div>
                </div>
              </div>
            </ScrollArea>

            {/* Input */}
            <div className="p-4 border-t">
              <div className="flex gap-2">
                <Button variant="ghost" size="sm">
                  <Smile className="h-5 w-5" />
                </Button>
                <Button variant="ghost" size="sm">
                  <Paperclip className="h-5 w-5" />
                </Button>
                <Input
                  placeholder="Nhập tin nhắn..."
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  onKeyPress={(e) => {
                    if (e.key === "Enter" && message.trim()) {
                      // Send message
                      setMessage("");
                    }
                  }}
                />
                <Button disabled={!message.trim()}>
                  <Send className="h-5 w-5" />
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
