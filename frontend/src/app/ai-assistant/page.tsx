'use client'

import { useRef, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useChat, UIMessage } from "@ai-sdk/react";
import { DefaultChatTransport } from "ai";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { MessageScroller } from "@/components/ai/MessageScroller";
import { ToolResultParts } from "@/components/ai/ToolResultParts";
import { friendlyAiError } from "@/components/ai/aiError";
import { getSession } from 'next-auth/react';
import { Bot, Send, Search, Calendar, TrendingUp, AlertCircle, StopCircle } from "lucide-react";

/** Message chỉ có tool parts (đang gọi tool / lỗi tool) — không render bubble text rỗng. */
function messageHasText(msg: UIMessage) {
  return msg.parts.some(
    (part: any) => part.type === 'text' && typeof part.text === 'string' && part.text.trim().length > 0
  );
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
        <li key={index} className="list-disc ml-5 my-1 text-sm leading-relaxed">
          {contentNode}
        </li>
      );
    }
    if (isNumberedList) {
      const match = line.trim().match(/^(\d+)\.\s/);
      const num = match ? match[1] : "1";
      return (
        <li key={index} className="list-decimal ml-5 my-1 text-sm leading-relaxed" value={num}>
          {contentNode}
        </li>
      );
    }

    return (
      <p key={index} className="my-1 text-sm leading-relaxed min-h-[1rem]">
        {contentNode}
      </p>
    );
  });
}

function AIAssistantPage() {
  const router = useRouter();
  const [token, setToken] = useState<string | null>(null);
  const [sessionLoaded, setSessionLoaded] = useState(false);
  const [blockedForRole, setBlockedForRole] = useState(false);

  useEffect(() => {
    getSession().then((session) => {
      const accessToken = (session as any)?.accessToken;
      if (accessToken) {
        setToken(accessToken);
      }
      const roleName = session?.user?.roleName;
      // Trợ lý AI hiện chỉ có tool dành cho Customer (tìm sân/kèo ghép) — Owner/Admin
      // chưa có agent riêng nên chuyển hướng về trang chủ nếu cố truy cập trực tiếp.
      if (roleName === "Owner" || roleName === "Admin") {
        setBlockedForRole(true);
        router.replace("/");
        return;
      }
      setSessionLoaded(true);
    });
  }, [router]);

  if (blockedForRole) {
    return null;
  }

  if (!sessionLoaded) {
    return (
      <div className="h-screen flex flex-col bg-background">
        <Header />
        <div className="flex-1 flex items-center justify-center">
          <div className="animate-spin h-8 w-8 border-4 border-primary border-t-transparent rounded-full" />
        </div>
        <Footer />
      </div>
    );
  }

  return <AIAssistantPageInner token={token} sessionLoaded={sessionLoaded} />;
}

interface PageInnerProps {
  token: string | null;
  sessionLoaded: boolean;
}

function AIAssistantPageInner({ token, sessionLoaded }: PageInnerProps) {
  const [inputValue, setInputValue] = useState("");

  const { messages, sendMessage, status, error, stop } = useChat({
    transport: new DefaultChatTransport({
      api: '/api/v1/ai/chat',
      headers: token ? {
        'Authorization': `Bearer ${token}`
      } : {}
    }),
    messages: [
      {
        id: "welcome",
        role: "assistant",
        parts: [
          {
            type: "text",
            text: "Xin chào! Tôi là trợ lý ảo AI của SportHub. Tôi có thể giúp bạn tìm kiếm sân thể thao, tham khảo lịch hoạt động, bảng giá, hoặc hướng dẫn bạn cách đặt lịch và thanh toán trên hệ thống. Hôm nay bạn cần tôi hỗ trợ thông tin gì?",
            state: "done"
          }
        ]
      }
    ] as UIMessage[],
  });

  const isLoading = status === "submitted" || status === "streaming";

  const quickActions = [
    { icon: <Search className="h-4 w-4" />, text: "Tìm sân bóng đá gần tôi" },
    { icon: <Calendar className="h-4 w-4" />, text: "Hướng dẫn thanh toán VNPay" },
    { icon: <TrendingUp className="h-4 w-4" />, text: "Chính sách hủy đặt sân" },
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
    <div className="h-screen flex flex-col bg-background">
      <Header />

      <div className="flex-1 container mx-auto px-4 py-8 overflow-hidden flex flex-col max-w-4xl">
        <div className="mb-6 text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-primary/10 rounded-full mb-3 relative">
            <Bot className="h-8 w-8 text-primary" />
            <span className="absolute bottom-0 right-0 w-4 h-4 bg-green-500 border-2 border-background rounded-full animate-pulse" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight mb-2">Trợ lý AI Đặt Sân</h1>
          <p className="text-muted-foreground max-w-md mx-auto text-sm">
            Trò chuyện trực tuyến 24/7 để nhận các thông tin hữu ích về dịch vụ và sân đấu tại SportHub.
          </p>
        </div>

        <Card className="flex-1 flex flex-col overflow-hidden border border-border/80 shadow-md">
          <MessageScroller dependencies={[messages, isLoading]}>
            <div className="space-y-6 pb-4">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`flex flex-col gap-3`}
                >
                  {(msg.role === "user" || messageHasText(msg)) && (
                  <div className={`flex gap-3 ${msg.role === "user" ? "flex-row-reverse" : ""}`}>
                    <Avatar className="h-10 w-10 flex-shrink-0 border border-border">
                      {msg.role === "assistant" ? (
                        <div className="w-full h-full bg-primary/10 flex items-center justify-center">
                          <Bot className="h-6 w-6 text-primary" />
                        </div>
                      ) : (
                        <AvatarFallback className="bg-secondary text-secondary-foreground font-semibold">U</AvatarFallback>
                      )}
                    </Avatar>

                    <div className={`flex-1 ${msg.role === "user" ? "text-right" : ""}`}>
                      <div
                        className={`inline-block max-w-[85%] text-left ${
                          msg.role === "user"
                            ? "bg-primary text-primary-foreground"
                            : "bg-muted text-foreground"
                        } rounded-lg px-4 py-3 shadow-sm`}
                      >
                        <div className="space-y-1">
                          {msg.parts.map((part: any, partIdx: number) => {
                            if (part.type === 'text') {
                              return <div key={partIdx}>{formatMessageContent(part.text)}</div>;
                            }
                            return null;
                          })}
                        </div>
                      </div>
                    </div>
                  </div>
                  )}

                  {/* Trạng thái/card kết quả tool-call — logic dùng chung ở ToolResultParts */}
                  {msg.role === "assistant" && msg.parts && (
                    <div className="md:pl-12 pl-2 space-y-3">
                      <ToolResultParts parts={msg.parts} />
                    </div>
                  )}
                </div>
              ))}

              {/* Typing Indicator */}
              {isLoading && messages[messages.length - 1]?.role === 'user' && (
                <div className="flex gap-3">
                  <Avatar className="h-10 w-10 flex-shrink-0 border border-border">
                    <div className="w-full h-full bg-primary/10 flex items-center justify-center">
                      <Bot className="h-6 w-6 text-primary" />
                    </div>
                  </Avatar>
                  <div className="bg-muted rounded-lg px-4 py-3 flex items-center space-x-1.5 shadow-sm">
                    <span className="sr-only">Trợ lý đang trả lời...</span>
                    <div className="w-2 h-2 bg-primary/60 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                    <div className="w-2 h-2 bg-primary/60 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                    <div className="w-2 h-2 bg-primary/60 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                  </div>
                </div>
              )}

              {/* Error display */}
              {error && (
                <div className="flex items-center gap-2 p-3 bg-destructive/10 text-destructive text-sm rounded-lg max-w-md mx-auto justify-center">
                  <AlertCircle className="h-4 w-4" />
                  <span>{friendlyAiError(error)}</span>
                </div>
              )}

              {/* Quick Actions (shown on first message) */}
              {messages.length === 1 && !isLoading && (
                <div className="flex flex-wrap gap-2 justify-center pt-2">
                  {quickActions.map((action, idx) => (
                    <Button
                      key={idx}
                      variant="outline"
                      size="sm"
                      className="gap-2 bg-background hover:bg-muted text-xs transition-colors rounded-full"
                      onClick={() => handleQuickAction(action.text)}
                    >
                      {action.icon}
                      {action.text}
                    </Button>
                  ))}
                </div>
              )}
            </div>
          </MessageScroller>

          {/* STOP Button */}
          {isLoading && (
            <div className="flex justify-center mb-2">
              <Button 
                variant="outline" 
                size="sm" 
                className="gap-2 rounded-full text-xs hover:bg-destructive/10 hover:text-destructive transition-colors"
                onClick={stop}
              >
                <StopCircle className="h-3.5 w-3.5" />
                Dừng câu trả lời
              </Button>
            </div>
          )}

          <form onSubmit={handleSubmit} className="p-4 border-t bg-card">
            <div className="flex gap-2">
              <Input
                placeholder={sessionLoaded ? "Hỏi tôi về sân, giá, hướng dẫn thanh toán..." : "Đang tải phiên làm việc..."}
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                disabled={!sessionLoaded}
                className="flex-1 focus-visible:ring-primary/50"
              />
              <Button type="submit" disabled={!inputValue.trim() || isLoading || !sessionLoaded} className="rounded-full h-10 w-10 p-0 flex items-center justify-center shrink-0">
                <Send className="h-4 w-4" />
                <span className="sr-only">Gửi</span>
              </Button>
            </div>
          </form>
        </Card>
      </div>

      <Footer />
    </div>
  );
}

export default AIAssistantPage;
