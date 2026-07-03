'use client'

import { useRef, useEffect, useState } from "react";
import { useChat, UIMessage } from "@ai-sdk/react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/landing/Footer";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Bot, Send, Search, Calendar, TrendingUp, Sparkles, AlertCircle } from "lucide-react";

function getMessageText(msg: UIMessage) {
  return msg.parts
    .filter(part => part.type === 'text')
    .map(part => (part as any).text)
    .join('');
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
  const [inputValue, setInputValue] = useState("");
  const { messages, sendMessage, status, error } = useChat({
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
      },
    ] as UIMessage[],
  });

  const isLoading = status === "submitted" || status === "streaming";
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const quickActions = [
    { icon: <Search className="h-4 w-4" />, text: "Tìm sân bóng đá gần tôi" },
    { icon: <Calendar className="h-4 w-4" />, text: "Hướng dẫn thanh toán VNPay" },
    { icon: <TrendingUp className="h-4 w-4" />, text: "Chính sách hủy đặt sân" },
  ];

  const venueRecommendation = {
    name: "Sân Bóng Đá SportHub Thủ Đức",
    sportType: "Bóng đá 5/7 người",
    price: 300000,
    rating: 4.8,
    location: "Trường Thọ, Thủ Đức, TP.HCM",
    availableSlots: "Còn nhiều khung giờ trống",
  };

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
          <ScrollArea className="flex-1 p-6">
            <div className="space-y-6 pb-4">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`flex gap-3 ${
                    msg.role === "user" ? "flex-row-reverse" : ""
                  }`}
                >
                  <Avatar className="h-10 w-10 flex-shrink-0 border border-border">
                    {msg.role === "assistant" ? (
                      <div className="w-full h-full bg-primary/10 flex items-center justify-center">
                        <Bot className="h-6 w-6 text-primary" />
                      </div>
                    ) : (
                      <AvatarFallback className="bg-secondary text-secondary-foreground font-semibold">U</AvatarFallback>
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
                      } rounded-lg px-4 py-3 shadow-sm`}
                    >
                      <div className="space-y-1">
                        {formatMessageContent(getMessageText(msg))}
                      </div>
                    </div>
                  </div>
                </div>
              ))}

              {/* Typing Indicator */}
              {isLoading && messages[messages.length - 1]?.role === "user" && (
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
                  <span>Đã xảy ra lỗi khi kết nối với máy chủ AI.</span>
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

              {/* Venue Recommendation Card */}
              {messages.length > 2 && (
                <div className="flex justify-start md:pl-12 pl-2">
                  <Card className="max-w-sm border border-border/80 shadow-md bg-card overflow-hidden transition-all hover:shadow-lg">
                    <div className="h-32 bg-gradient-to-r from-emerald-500 to-teal-600 flex items-center justify-center text-white relative">
                      <Sparkles className="absolute top-2 right-2 text-yellow-300 h-5 w-5 animate-pulse" />
                      <div className="text-center p-4">
                        <h4 className="font-bold text-lg leading-tight">{venueRecommendation.name}</h4>
                        <p className="text-xs opacity-90 mt-1">{venueRecommendation.location}</p>
                      </div>
                    </div>
                    <CardContent className="p-4 space-y-3">
                      <div className="flex justify-between items-center">
                        <Badge variant="secondary" className="bg-teal-500/10 text-teal-600 dark:text-teal-400 hover:bg-teal-500/20 border-none">
                          {venueRecommendation.sportType}
                        </Badge>
                        <span className="text-sm font-semibold text-yellow-500">
                          ⭐ {venueRecommendation.rating}
                        </span>
                      </div>

                      <div className="text-xs text-muted-foreground flex justify-between">
                        <span>Trạng thái:</span>
                        <span className="font-medium text-emerald-600 dark:text-emerald-400">
                          {venueRecommendation.availableSlots}
                        </span>
                      </div>

                      <div className="border-t pt-3 flex items-center justify-between">
                        <div>
                          <p className="text-xs text-muted-foreground">Giá từ</p>
                          <span className="text-lg font-bold text-primary">
                            {venueRecommendation.price.toLocaleString('vi-VN')}đ<span className="text-xs font-normal text-muted-foreground">/giờ</span>
                          </span>
                        </div>
                        <Button size="sm" className="bg-primary hover:bg-primary/95 text-xs rounded-full">
                          Đặt sân ngay
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                </div>
              )}
              
              <div ref={messagesEndRef} />
            </div>
          </ScrollArea>

          <form onSubmit={handleSubmit} className="p-4 border-t bg-card">
            <div className="flex gap-2">
              <Input
                placeholder="Hỏi tôi về sân, giá, hướng dẫn thanh toán..."
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                className="flex-1 focus-visible:ring-primary/50"
              />
              <Button type="submit" disabled={!inputValue.trim() || isLoading} className="rounded-full h-10 w-10 p-0 flex items-center justify-center shrink-0">
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
