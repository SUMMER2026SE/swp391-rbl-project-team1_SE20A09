"use client";

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Bot } from "lucide-react";
import { useTypewriter } from "@/hooks/useTypewriter";
import { MessageItem } from "@/hooks/useAiChat";
import { StadiumResultCard } from "@/components/ai-assistant/StadiumResultCard";
import { SlotResultCard } from "@/components/ai-assistant/SlotResultCard";
import { MatchResultCard } from "@/components/ai-assistant/MatchResultCard";

interface ChatMessageProps {
  msg: MessageItem;
  isLatest: boolean;
}

export function ChatMessageItem({ msg, isLatest }: ChatMessageProps) {
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
            className={`inline-block max-w-xs md:max-w-lg ${
              msg.type === "user"
                ? "bg-primary text-primary-foreground rounded-2xl rounded-tr-sm"
                : "bg-muted rounded-2xl rounded-tl-sm text-foreground"
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
          <div className="w-full flex flex-col gap-3 mt-1 overflow-x-auto max-w-[270px] xs:max-w-xs sm:max-w-md md:max-w-lg scrollbar-thin">
            {msg.stadiums && msg.stadiums.length > 0 && (
              <div className="flex flex-col sm:flex-row flex-wrap gap-3">
                {msg.stadiums.map((stadium) => (
                  <StadiumResultCard key={stadium.stadiumId} stadium={stadium} />
                ))}
              </div>
            )}

            {msg.slots && msg.slots.length > 0 && (
              <SlotResultCard slots={msg.slots} />
            )}

            {msg.matches && msg.matches.length > 0 && (
              <div className="flex flex-col sm:flex-row flex-wrap gap-3">
                {msg.matches.map((match) => (
                  <MatchResultCard key={match.matchId} match={match} />
                ))}
              </div>
            )}

            {/* policyText là nội dung chính sách CHUẨN XÁC từ backend (PolicyHandler) */}
            {msg.policyText && (
              <div className="w-full max-w-xs md:max-w-lg rounded-xl border border-border/80 bg-muted/30 p-3">
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
