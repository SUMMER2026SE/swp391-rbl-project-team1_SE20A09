'use client'

import { useState, useRef, useEffect } from "react";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Skeleton } from "@/components/ui/skeleton";
import { Bot, Send, Trash2 } from "lucide-react";
import { useTypewriter } from "@/hooks/useTypewriter";
import { sendChatMessage } from "@/lib/ai-chat-api";
import { ChatMessage, TimeSlotResponse } from "@/types/aiChat";
import { StadiumResponse } from "@/types/stadium";
import { MatchResponse } from "@/types/match";
import { StadiumResultCard } from "@/components/ai-assistant/StadiumResultCard";
import { SlotResultCard } from "@/components/ai-assistant/SlotResultCard";
import { MatchResultCard } from "@/components/ai-assistant/MatchResultCard";
import { SuggestionChips } from "@/components/ai-assistant/SuggestionChips";

interface MessageItem {
  id: string | number;
  type: string; // "user" | "assistant"
  content: string;
  timestamp: string;
  stadiums?: StadiumResponse[] | null;
  slots?: TimeSlotResponse[] | null;
  matches?: MatchResponse[] | null;
  policyText?: string | null;
  isHistory?: boolean; // Cờ đánh dấu tin nhắn load từ lịch sử cũ, không chạy lại typewriter
}

interface ChatMessageProps {
  msg: MessageItem;
  isLatest: boolean;
}

function ChatMessageItem({ msg, isLatest }: ChatMessageProps) {
  const isAssistant = msg.type === "assistant";
  // Chỉ chạy typewriter nếu là tin nhắn AI mới nhất VÀ không phải load từ lịch sử
  const shouldAnimate = isAssistant && isLatest && !msg.isHistory;

  const { displayText, isTyping } = useTypewriter(
    shouldAnimate ? msg.content : "",
    15
  );

  const displayedContent = shouldAnimate ? (isTyping ? displayText : msg.content) : msg.content;

  return (
    <div className={`flex gap-3 ${msg.type === "user" ? "flex-row-reverse" : ""}`}>
      <Avatar className="h-10 w-10 flex-shrink-0">
        {isAssistant ? (
          <div className="w-full h-full bg-primary/10 flex items-center justify-center">
            <Bot className="h-6 w-6 text-primary" />
          </div>
        ) : (
          <AvatarFallback className="bg-primary text-primary-foreground">U</AvatarFallback>
        )}
      </Avatar>

      <div className={`flex-1 flex flex-col gap-2 ${msg.type === "user" ? "items-end" : "items-start"}`}>
        <div>
          <div
            className={`inline-block max-w-md md:max-w-lg ${
              msg.type === "user"
                ? "bg-primary text-primary-foreground rounded-2xl rounded-tr-sm"
                : "bg-muted rounded-2xl rounded-tl-sm"
            } px-4 py-3`}
          >
            <p className="text-sm whitespace-pre-line leading-relaxed">{displayedContent}</p>
          </div>
          <p className="text-[10px] text-muted-foreground mt-1 px-1">
            {msg.timestamp}
          </p>
        </div>

        {/* Render cards immediately */}
        {isAssistant && (
          <div className="w-full flex flex-col gap-3 mt-1">
            {msg.stadiums && msg.stadiums.length > 0 && (
              <div className="flex flex-wrap gap-3">
                {msg.stadiums.map((stadium) => (
                  <StadiumResultCard key={stadium.stadiumId} stadium={stadium} />
                ))}
              </div>
            )}

            {msg.slots && msg.slots.length > 0 && (
              <SlotResultCard slots={msg.slots} />
            )}

            {msg.matches && msg.matches.length > 0 && (
              <div className="flex flex-wrap gap-3">
                {msg.matches.map((match) => (
                  <MatchResultCard key={match.matchId} match={match} />
                ))}
              </div>
            )}

            {/* policyText là nội dung chính sách CHUẨN XÁC từ backend (PolicyHandler) — luôn
                hiển thị tách biệt, không dựa vào lời LLM tự diễn giải ở content phía trên
                (LLM có thể nhớ nhầm/diễn giải sai nội dung FAQ). */}
            {msg.policyText && (
              <div className="w-full max-w-md md:max-w-lg rounded-xl border border-border/80 bg-muted/30 p-3">
                <p className="text-xs leading-relaxed text-foreground whitespace-pre-line">
                  {msg.policyText}
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function AIAssistantPage() {
  const [message, setMessage] = useState("");
  const [isSearching, setIsSearching] = useState(false);
  const [messages, setMessages] = useState<MessageItem[]>([]);

  // Đọc lịch sử trò chuyện từ sessionStorage khi trang vừa mount
  useEffect(() => {
    if (typeof window !== "undefined") {
      const stored = sessionStorage.getItem("ai_chat_messages");
      if (stored) {
        try {
          const parsed = JSON.parse(stored) as MessageItem[];
          // Đánh dấu toàn bộ tin nhắn lịch sử là isHistory = true để tránh typewriter lặp lại
          setMessages(parsed.map(m => ({ ...m, isHistory: true })));
          return;
        } catch (e) {
          // Fallback to default
        }
      }
    }
    // Lịch sử mặc định nếu chưa có
    setMessages([
      {
        id: "welcome",
        type: "assistant",
        content:
          "Xin chào! Tôi là trợ lý AI của SportsBook. Tôi có thể giúp bạn tìm sân, đặt lịch, hoặc gợi ý sân phù hợp. Bạn cần tôi giúp gì?",
        timestamp: new Date().toLocaleTimeString("vi-VN", {
          hour: "2-digit",
          minute: "2-digit",
        }),
      },
    ]);
  }, []);

  // Ghi lịch sử trò chuyện vào sessionStorage mỗi khi tin nhắn thay đổi
  useEffect(() => {
    if (typeof window !== "undefined" && messages.length > 0) {
      sessionStorage.setItem("ai_chat_messages", JSON.stringify(messages));
    }
  }, [messages]);

  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isSearching]);

  const handleSend = async () => {
    const q = message.trim();
    if (!q || isSearching) return;

    const userTime = new Date().toLocaleTimeString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
    });

    const userMessage: MessageItem = {
      id: "user-" + Date.now(),
      type: "user",
      content: q,
      timestamp: userTime,
    };

    setMessages((prev) => [...prev, userMessage]);
    setMessage("");
    setIsSearching(true);

    try {
      // Khi gửi tin mới, toàn bộ tin nhắn hiện hữu chuyển thành history
      const historyPayload: ChatMessage[] = messages.map((m) => ({
        role: m.type === "assistant" ? "assistant" : "user",
        content: m.content,
      }));

      const result = await sendChatMessage(q, historyPayload);

      const aiResponse: MessageItem = {
        id: "ai-" + Date.now(),
        type: "assistant",
        content: result.message || "",
        timestamp: new Date().toLocaleTimeString("vi-VN", {
          hour: "2-digit",
          minute: "2-digit",
        }),
        stadiums: result.stadiums,
        slots: result.slots,
        matches: result.matches,
        policyText: result.policyText,
        isHistory: false, // Tin nhắn mới, kích hoạt typewriter
      };

      setMessages((prev) => [...prev, aiResponse]);
    } catch (error) {
      const errorMsg: MessageItem = {
        id: "err-" + Date.now(),
        type: "assistant",
        content: "Không thể kết nối tới máy chủ. Vui lòng kiểm tra lại kết nối mạng của bạn.",
        timestamp: new Date().toLocaleTimeString("vi-VN", {
          hour: "2-digit",
          minute: "2-digit",
        }),
        isHistory: false,
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsSearching(false);
    }
  };

  const handleClearHistory = () => {
    if (typeof window !== "undefined") {
      sessionStorage.removeItem("ai_chat_messages");
      sessionStorage.removeItem("ai_session_id"); // Reset session ID trên Redis context
    }
    setMessages([
      {
        id: "welcome-" + Date.now(),
        type: "assistant",
        content:
          "Xin chào! Tôi là trợ lý AI của SportsBook. Tôi có thể giúp bạn tìm sân, đặt lịch, hoặc gợi ý sân phù hợp. Bạn cần tôi giúp gì?",
        timestamp: new Date().toLocaleTimeString("vi-VN", {
          hour: "2-digit",
          minute: "2-digit",
        }),
      },
    ]);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="h-screen flex flex-col bg-background">
      <Header />

      <div className="flex-1 container mx-auto px-4 py-6 overflow-hidden flex flex-col max-w-4xl">
        <div className="mb-4 text-center shrink-0">
          <div className="inline-flex items-center justify-center w-12 h-12 bg-primary/10 rounded-full mb-2">
            <Bot className="h-6 w-6 text-primary" />
          </div>
          <h1 className="text-xl font-bold">Trợ lý AI đặt sân</h1>
          <p className="text-xs text-muted-foreground">
            Hỏi tôi bất cứ điều gì về sân thể thao, giá cả, lịch trống và kèo ghép
          </p>
        </div>

        <Card className="flex-1 flex flex-col overflow-hidden border-border/80 shadow-sm rounded-xl">
          {/* Header Card chứa nút Xóa Lịch sử */}
          <div className="px-6 py-3 border-b border-border/60 flex justify-between items-center bg-muted/10 shrink-0">
            <div className="flex items-center gap-2">
              <Bot className="h-4 w-4 text-primary animate-pulse" />
              <span className="text-xs font-semibold text-gray-700">SportsBook Assistant</span>
            </div>
            {messages.length > 1 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={handleClearHistory}
                className="text-xs text-muted-foreground hover:text-destructive hover:bg-destructive/5 gap-1.5 h-8 px-2.5 rounded-lg"
              >
                <Trash2 className="h-3.5 w-3.5" />
                Xóa lịch sử
              </Button>
            )}
          </div>

          <ScrollArea className="flex-1 min-h-0 p-6">
            <div className="space-y-6">
              {messages.map((msg, idx) => (
                <ChatMessageItem
                  key={msg.id}
                  msg={msg}
                  isLatest={idx === messages.length - 1}
                />
              ))}

              {/* Hiện Suggestion Chips khi chỉ có 1 lời chào */}
              {messages.length === 1 && !isSearching && (
                <SuggestionChips onSelect={(text) => setMessage(text)} />
              )}

              {/* Skeleton loading bubble */}
              {isSearching && (
                <div className="flex gap-3">
                  <Avatar className="h-10 w-10 flex-shrink-0">
                    <div className="w-full h-full bg-primary/10 flex items-center justify-center">
                      <Bot className="h-6 w-6 text-primary" />
                    </div>
                  </Avatar>
                  <div className="flex-1 flex flex-col gap-2">
                    <div className="inline-block bg-muted rounded-2xl rounded-tl-sm px-4 py-3 max-w-[120px]">
                      <div className="flex space-x-1.5 items-center justify-center h-4">
                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                        <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                      </div>
                    </div>
                    <div className="w-full max-w-sm space-y-3 mt-1">
                      <Skeleton className="h-32 w-full rounded-xl bg-muted/65" />
                    </div>
                  </div>
                </div>
              )}
              <div ref={chatEndRef} />
            </div>
          </ScrollArea>

          <div className="p-4 border-t border-border/60 bg-muted/20 shrink-0">
            <div className="flex gap-2 max-w-3xl mx-auto">
              <Input
                placeholder="Hỏi tôi về sân, giá, lịch trống, kèo ghép..."
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={isSearching}
                className="rounded-xl border-border/80 focus-visible:ring-primary h-10"
              />
              <Button onClick={handleSend} disabled={!message.trim() || isSearching} className="rounded-xl h-10 px-4">
                <Send className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}

export default AIAssistantPage;
