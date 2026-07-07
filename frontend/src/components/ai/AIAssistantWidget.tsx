'use client'

import { useState, useRef, useEffect } from "react";
import { usePathname } from "next/navigation";
import { useChat, UIMessage } from "@ai-sdk/react";
import { DefaultChatTransport } from "ai";
import { getSession } from 'next-auth/react';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardFooter, CardHeader } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { MessageScroller } from "./MessageScroller";
import { ToolResultParts } from "./ToolResultParts";
import { friendlyAiError } from "./aiError";
import { Bot, Send, X, MessageSquare, Search, Calendar, TrendingUp, AlertCircle } from "lucide-react";

function getMessageText(msg: UIMessage) {
  return msg.parts
    .filter(part => part.type === 'text')
    .map(part => (part as any).text)
    .join('');
}

/** Message chỉ có tool parts (đang gọi tool / lỗi tool) — không render bubble text rỗng. */
function messageHasText(msg: UIMessage) {
  return getMessageText(msg).trim().length > 0;
}

function formatMessageContent(content: string) {
  const lines = content.split('\n');
  return lines.map((line, index) => {
    const isBulletList = line.trim().startsWith('- ') || line.trim().startsWith('* ');
    const isNumberedList = /^\d+\.\s/.test(line.trim());

    let cleanText = line;
    if (isBulletList) {
      cleanText = line.trim().substring(2);
    } else if (isNumberedList) {
      cleanText = line.trim().replace(/^\d+\.\s/, '');
    }

    const parts = cleanText.split(/\*\*([^*]+)\*\*/g);
    const contentNode = parts.map((part, i) => {
      if (i % 2 === 1) {
        return <strong key={i} className="font-semibold text-foreground">{part}</strong>;
      }
      return part;
    });

    if (isBulletList) {
      return (
        <li key={index} className="list-disc ml-4 my-0.5 text-xs leading-relaxed">
          {contentNode}
        </li>
      );
    }
    if (isNumberedList) {
      const match = line.trim().match(/^(\d+)\.\s/);
      const num = match ? match[1] : "1";
      return (
        <li key={index} className="list-decimal ml-4 my-0.5 text-xs leading-relaxed" value={num}>
          {contentNode}
        </li>
      );
    }

    return (
      <p key={index} className="my-0.5 text-xs leading-relaxed min-h-[0.75rem]">
        {contentNode}
      </p>
    );
  });
}

export default function AIAssistantWidget() {
  const pathname = usePathname();
  const [isOpen, setIsOpen] = useState(false);
  const [token, setToken] = useState<string | null>(null);
  const [roleName, setRoleName] = useState<string | null>(null);
  const [sessionLoaded, setSessionLoaded] = useState(false);

  useEffect(() => {
    getSession().then((session) => {
      const accessToken = (session as any)?.accessToken;
      if (accessToken) {
        setToken(accessToken);
      }
      setRoleName(session?.user?.roleName ?? null);
      setSessionLoaded(true);
    });
  }, []);

  // Hide the floating widget if user is already on the main AI Assistant page
  if (pathname === "/ai-assistant") return null;

  if (!sessionLoaded) {
    return null;
  }

  // Trợ lý AI hiện chỉ có tool dành cho Customer (tìm sân/kèo ghép) — Owner/Admin
  // chưa có agent riêng nên ẩn widget để tránh gây hiểu lầm là dùng được cho việc quản lý.
  if (roleName === "Owner" || roleName === "Admin") {
    return null;
  }

  return (
    <AIAssistantWidgetInner
      token={token}
      sessionLoaded={sessionLoaded}
      isOpen={isOpen}
      setIsOpen={setIsOpen}
    />
  );
}

interface WidgetInnerProps {
  token: string | null;
  sessionLoaded: boolean;
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
}

function AIAssistantWidgetInner({ token, sessionLoaded, isOpen, setIsOpen }: WidgetInnerProps) {
  const [inputValue, setInputValue] = useState("");

  const { messages, sendMessage, status, error } = useChat({
    transport: new DefaultChatTransport({
      api: '/api/v1/ai/chat',
      headers: token ? {
        'Authorization': `Bearer ${token}`
      } : {}
    }),
    messages: [
      {
        id: "widget-welcome",
        role: "assistant",
        parts: [
          {
            type: "text",
            text: "Xin chào! 👋 Tôi là trợ lý ảo AI của SportHub. Bạn cần hỗ trợ tìm sân hay giải đáp thắc mắc gì không?",
            state: "done"
          }
        ]
      },
    ] as UIMessage[],
  });

  const isLoading = status === "submitted" || status === "streaming";

  const quickActions = [
    { icon: <Search className="h-3 w-3" />, text: "Tìm sân gần đây" },
    { icon: <Calendar className="h-3 w-3" />, text: "Cách đặt lịch" },
    { icon: <TrendingUp className="h-3 w-3" />, text: "Chính sách hủy sân" },
  ];

  const handleQuickAction = (text: string) => {
    sendMessage({ text });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputValue.trim() || isLoading) return;
    sendMessage({ text: inputValue });
    setInputValue("");
  };

  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end font-sans">
      {/* Chat Window Panel */}
      {isOpen && (
        <Card className="mb-4 w-[360px] flex flex-col shadow-2xl border border-border/80 rounded-2xl overflow-clip animate-in slide-in-from-bottom-5 duration-200 bg-card" style={{ height: 'min(500px, calc(100vh - 8rem))' }}>
          {/* Header */}
          <CardHeader className="p-4 bg-primary text-primary-foreground flex flex-row items-center justify-between space-y-0 shrink-0">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 bg-white/20 rounded-full flex items-center justify-center relative">
                <Bot className="h-5 w-5 text-white" />
                <span className="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-400 border-2 border-primary rounded-full animate-pulse" />
              </div>
              <div>
                <h3 className="font-bold text-sm leading-tight">Trợ lý SportHub</h3>
                <p className="text-[10px] text-primary-foreground/80 font-normal">Đang hoạt động</p>
              </div>
            </div>
            <Button
              variant="ghost"
              size="icon"
              className="text-primary-foreground hover:bg-white/10 rounded-full h-8 w-8 shrink-0"
              onClick={() => setIsOpen(false)}
            >
              <X className="h-4 w-4" />
              <span className="sr-only">Đóng</span>
            </Button>
          </CardHeader>

          {/* Messages body — flex-1 để chiếm toàn bộ không gian còn lại */}
          <MessageScroller dependencies={[messages, isLoading]}>
            <div className="space-y-4 pb-2">
              {messages.map((msg) => (
                <div key={msg.id} className="flex flex-col gap-2">
                  {(msg.role === "user" || messageHasText(msg)) && (
                  <div
                    className={`flex gap-2.5 ${
                      msg.role === "user" ? "flex-row-reverse" : ""
                    }`}
                  >
                    <Avatar className="h-8 w-8 flex-shrink-0 border border-border">
                      {msg.role === "assistant" ? (
                        <div className="w-full h-full bg-primary/10 flex items-center justify-center">
                          <Bot className="h-5 w-5 text-primary" />
                        </div>
                      ) : (
                        <AvatarFallback className="bg-secondary text-secondary-foreground text-[10px] font-bold">U</AvatarFallback>
                      )}
                    </Avatar>

                    <div
                      className={`flex-1 ${
                        msg.role === "user" ? "text-right" : ""
                      }`}
                    >
                      <div
                        className={`inline-block max-w-[85%] text-left ${
                          msg.role === "user"
                            ? "bg-primary text-primary-foreground"
                            : "bg-muted text-foreground"
                        } rounded-2xl px-3 py-2 shadow-sm`}
                      >
                        <div className="space-y-1">
                          {formatMessageContent(getMessageText(msg))}
                        </div>
                      </div>
                    </div>
                  </div>
                  )}

                  {msg.role === "assistant" && msg.parts && (
                    <div className="pl-10 space-y-2">
                      <ToolResultParts parts={msg.parts} compact />
                    </div>
                  )}
                </div>
              ))}

              {/* Typing/Loading indicator */}
              {isLoading && messages[messages.length - 1]?.role === "user" && (
                <div className="flex gap-2.5">
                  <Avatar className="h-8 w-8 flex-shrink-0 border border-border">
                    <div className="w-full h-full bg-primary/10 flex items-center justify-center">
                      <Bot className="h-5 w-5 text-primary" />
                    </div>
                  </Avatar>
                  <div className="bg-muted rounded-2xl px-3 py-2 flex items-center space-x-1.5 shadow-sm">
                    <span className="sr-only">Đang trả lời...</span>
                    <div className="w-1.5 h-1.5 bg-primary/60 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                    <div className="w-1.5 h-1.5 bg-primary/60 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                    <div className="w-1.5 h-1.5 bg-primary/60 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                  </div>
                </div>
              )}

              {/* Error display */}
              {error && (
                <div className="flex items-center gap-1.5 p-2 bg-destructive/10 text-destructive text-[11px] rounded-lg max-w-[90%] mx-auto justify-center">
                  <AlertCircle className="h-3.5 w-3.5 shrink-0" />
                  <span>{friendlyAiError(error)}</span>
                </div>
              )}

              {/* Quick action buttons */}
              {messages.length === 1 && !isLoading && (
                <div className="flex flex-col gap-1.5 pt-2 pl-10 max-w-[85%]">
                  <p className="text-[10px] text-muted-foreground font-medium mb-0.5">Gợi ý câu hỏi:</p>
                  {quickActions.map((action, idx) => (
                    <Button
                      key={idx}
                      variant="outline"
                      size="sm"
                      className="justify-start gap-2 bg-background hover:bg-muted text-[11px] h-7 px-3 w-full rounded-lg transition-colors border border-border"
                      onClick={() => handleQuickAction(action.text)}
                    >
                      {action.icon}
                      <span className="truncate">{action.text}</span>
                    </Button>
                  ))}
                </div>
              )}

            </div>
          </MessageScroller>

          {/* Input Footer */}
          <CardFooter className="p-3 border-t bg-card shrink-0">
            <form onSubmit={handleSubmit} className="flex gap-2 w-full">
              <Input
                placeholder={!sessionLoaded ? "Đang kiểm tra phiên làm việc..." : "Nhập tin nhắn..."}
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                disabled={!sessionLoaded}
                className="flex-1 text-xs h-9 rounded-xl focus-visible:ring-primary/50"
              />
              <Button
                type="submit"
                disabled={!inputValue.trim() || isLoading}
                className="rounded-xl h-9 w-9 p-0 flex items-center justify-center shrink-0 bg-primary hover:bg-primary/95 text-primary-foreground"
              >
                <Send className="h-3.5 w-3.5" />
                <span className="sr-only">Gửi</span>
              </Button>
            </form>
          </CardFooter>
        </Card>
      )}

      {/* Floating Action Button */}
      <Button
        size="icon"
        className="h-14 w-14 rounded-full shadow-2xl hover:scale-105 transition-transform duration-200 bg-primary text-primary-foreground border-2 border-background hover:bg-primary/95 relative group"
        onClick={() => setIsOpen(!isOpen)}
      >
        {isOpen ? (
          <X className="h-6 w-6" />
        ) : (
          <>
            <MessageSquare className="h-6 w-6" />
            <span className="absolute -top-1 -right-1 flex h-4 w-4">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75" />
              <span className="relative inline-flex rounded-full h-4 w-4 bg-primary text-[8px] font-bold items-center justify-center text-primary-foreground border border-background">
                AI
              </span>
            </span>
          </>
        )}
        <span className="sr-only">Trợ lý SportHub AI</span>
      </Button>
    </div>
  );
}
