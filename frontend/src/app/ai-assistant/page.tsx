'use client'

import { useRef, useEffect, useState } from "react";
import { useChat, UIMessage } from "@ai-sdk/react";
import { DefaultChatTransport } from "ai";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { MessageScroller } from "@/components/ai/MessageScroller";
import { getSession } from 'next-auth/react';
import { Bot, Send, Search, Calendar, TrendingUp, Sparkles, AlertCircle, StopCircle } from "lucide-react";

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
  const [token, setToken] = useState<string | null>(null);
  const [sessionLoaded, setSessionLoaded] = useState(false);

  useEffect(() => {
    getSession().then((session) => {
      const accessToken = (session as any)?.accessToken;
      if (accessToken) {
        setToken(accessToken);
      }
      setSessionLoaded(true);
    });
  }, []);

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

                  {/* Render Tool Invocations inside message context */}
                  {msg.role === "assistant" && msg.parts && (
                    <div className="md:pl-12 pl-2 space-y-3">
                      {msg.parts.map((part: any, partIdx: number) => {
                        if (part.type === 'tool-call') {
                          const { toolCallId, toolName } = part.toolCall;
                          return (
                            <div key={toolCallId || partIdx} className="text-xs text-muted-foreground italic flex items-center gap-2 my-1">
                              <span className="animate-spin h-3.5 w-3.5 border-2 border-primary border-t-transparent rounded-full" />
                              Đang xử lý: {toolName === 'searchStadiums' ? 'Tìm kiếm sân đấu...' : toolName === 'getStadiumSlots' ? 'Tra cứu khung giờ trống...' : 'Tìm kèo ghép thể thao...'}
                            </div>
                          );
                        }

                        if (part.type === 'tool-result') {
                          const { toolCallId, toolName, result } = part.toolResult;
                          
                          if (toolName === 'searchStadiums' && Array.isArray(result)) {
                            if (result.length === 0) {
                              return (
                                <div key={toolCallId || partIdx} className="text-xs text-yellow-600 italic my-1">
                                  Không tìm thấy sân đấu nào phù hợp với yêu cầu.
                                </div>
                              );
                            }
                            return (
                              <div key={toolCallId || partIdx} className="grid gap-3 grid-cols-1 sm:grid-cols-2 mt-2">
                                {result.map((stadium: any) => (
                                  <Card key={stadium.stadiumId} className="border border-border/85 shadow-sm bg-card overflow-hidden transition-all hover:shadow-lg">
                                    <div className="h-20 bg-gradient-to-r from-emerald-500 to-teal-600 flex flex-col justify-center text-white relative p-3">
                                      <Sparkles className="absolute top-2 right-2 text-yellow-300 h-4.5 w-4.5 animate-pulse" />
                                      <h4 className="font-bold text-xs leading-tight line-clamp-1">{stadium.stadiumName}</h4>
                                      <p className="text-[9px] opacity-90 mt-0.5 line-clamp-1">{stadium.address}</p>
                                    </div>
                                    <CardContent className="p-3 space-y-2">
                                      <div className="flex justify-between items-center text-xs">
                                        <Badge variant="secondary" className="bg-teal-500/10 text-teal-600 dark:text-teal-400 border-none px-1.5 py-0 text-[10px]">
                                          {stadium.sportName || 'Thể thao'}
                                        </Badge>
                                        <span className="text-[10px] font-medium text-emerald-600">
                                          {stadium.status === 'AVAILABLE' ? 'Đang hoạt động' : 'Bảo trì'}
                                        </span>
                                      </div>
                                      <div className="border-t pt-2 flex items-center justify-between">
                                        <div>
                                          <span className="text-xs font-bold text-primary">
                                            {stadium.pricePerHour?.toLocaleString('vi-VN')}đ<span className="text-[9px] font-normal text-muted-foreground">/h</span>
                                          </span>
                                        </div>
                                        <Button 
                                          size="sm" 
                                          className="bg-primary hover:bg-primary/95 text-[10px] h-7 px-3 rounded-full"
                                          onClick={() => window.location.href = `/stadiums/${stadium.stadiumId}`}
                                        >
                                          Đặt ngay
                                        </Button>
                                      </div>
                                    </CardContent>
                                  </Card>
                                ))}
                              </div>
                            );
                          }

                          return (
                            <div key={toolCallId || partIdx} className="text-[11px] text-emerald-600 italic my-1 flex items-center gap-1.5">
                              <span>✓</span>
                              <span>Hoàn thành: {toolName === 'searchStadiums' ? 'Tìm sân đấu' : toolName === 'getStadiumSlots' ? 'Tra cứu khung giờ trống' : 'Tìm kèo ghép'}</span>
                            </div>
                          );
                        }

                        return null;
                      })}
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
                  <span>{error.message || 'Đã xảy ra lỗi khi kết nối với máy chủ AI.'}</span>
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
