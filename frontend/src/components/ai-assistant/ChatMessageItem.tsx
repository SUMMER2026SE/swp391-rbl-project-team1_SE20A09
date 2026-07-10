"use client";

import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Bot } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CalendarCheck, Users } from "lucide-react";
import Link from "next/link";
import { useTypewriter } from "@/hooks/useTypewriter";
import { MessageItem } from "@/hooks/useAiChat";
import { StadiumResultCard } from "@/components/ai-assistant/StadiumResultCard";
import { SlotResultCard } from "@/components/ai-assistant/SlotResultCard";
import { MatchResultCard } from "@/components/ai-assistant/MatchResultCard";
import { DraftJoinMatchCard } from "@/components/ai-assistant/DraftJoinMatchCard";

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

            {/* Booking confirmation card */}
            {msg.bookingId && (
              <div className="w-full max-w-xs md:max-w-lg rounded-xl border border-green-200 bg-green-50 dark:bg-green-950/20 dark:border-green-800 p-4">
                <div className="flex items-center gap-2 mb-2">
                  <CalendarCheck className="h-5 w-5 text-green-600 dark:text-green-400" />
                  <span className="font-semibold text-sm text-green-800 dark:text-green-300">
                    Đặt sân thành công!
                  </span>
                </div>
                <p className="text-xs text-green-700 dark:text-green-400 mb-3">
                  Mã đặt sân: BK{String(msg.bookingId).padStart(6, "0")}
                </p>
                <Link href={`/booking/${msg.bookingId}`}>
                  <Button size="sm" className="w-full bg-green-600 hover:bg-green-700 text-white">
                    Xem chi tiết & Thanh toán
                  </Button>
                </Link>
              </div>
            )}

            {/* Draft Booking card */}
            {msg.draftBooking && (
              <div className="w-full max-w-xs md:max-w-lg rounded-xl border border-amber-200 bg-amber-50 dark:bg-amber-950/20 dark:border-amber-800 p-4 shadow-sm">
                <div className="flex items-center gap-2 mb-3">
                  <CalendarCheck className="h-5 w-5 text-amber-600 dark:text-amber-400" />
                  <span className="font-semibold text-sm text-amber-800 dark:text-amber-300">
                    Xác nhận thông tin đặt sân
                  </span>
                </div>
                <div className="bg-white/60 dark:bg-black/20 rounded-md p-3 mb-3 border border-amber-100 dark:border-amber-900/50">
                  <div className="flex flex-col gap-1.5 text-xs text-amber-900 dark:text-amber-200">
                    <div className="flex justify-between">
                      <span className="font-medium opacity-80">Sân:</span>
                      <span className="font-bold">{msg.draftBooking.stadiumName}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="font-medium opacity-80">Ngày:</span>
                      <span className="font-bold">{msg.draftBooking.date}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="font-medium opacity-80">Giờ:</span>
                      <span className="font-bold">{msg.draftBooking.startTime}</span>
                    </div>
                    <div className="flex justify-between mt-1 pt-1.5 border-t border-amber-200/50 dark:border-amber-800/50">
                      <span className="font-medium opacity-80">Tạm tính:</span>
                      <span className="font-bold text-amber-600 dark:text-amber-400">{msg.draftBooking.price?.toLocaleString('vi-VN')}đ</span>
                    </div>
                  </div>
                </div>
                <Link href={`/booking/new?venueId=${msg.draftBooking.stadiumId}&date=${msg.draftBooking.date}&slot=${msg.draftBooking.startTime}`}>
                  <Button size="sm" className="w-full bg-amber-500 hover:bg-amber-600 text-white font-medium shadow-sm transition-all hover:shadow-md">
                    Tiến hành Đặt Sân & Thanh toán
                  </Button>
                </Link>
              </div>
            )}

            {/* Join match confirmation card */}
            {msg.matchId && !msg.draftJoinMatch && (
              <div className="w-full max-w-xs md:max-w-lg rounded-xl border border-blue-200 bg-blue-50 dark:bg-blue-950/20 dark:border-blue-800 p-4">
                <div className="flex items-center gap-2 mb-2">
                  <Users className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                  <span className="font-semibold text-sm text-blue-800 dark:text-blue-300">
                    Đã gửi yêu cầu tham gia kèo!
                  </span>
                </div>
                <p className="text-xs text-blue-700 dark:text-blue-400 mb-3">
                  Đang chờ chủ kèo xác nhận. Bạn sẽ được thông báo khi có kết quả.
                </p>
                <Link href={`/community`}>
                  <Button size="sm" variant="outline" className="w-full border-blue-300 text-blue-700 hover:bg-blue-100">
                    Xem kèo của tôi
                  </Button>
                </Link>
              </div>
            )}

            {/* Draft Join Match Card */}
            {msg.draftJoinMatch && (
              <DraftJoinMatchCard draftJoinMatch={msg.draftJoinMatch} />
            )}
          </div>
        )}
      </div>
    </div>
  );
}
