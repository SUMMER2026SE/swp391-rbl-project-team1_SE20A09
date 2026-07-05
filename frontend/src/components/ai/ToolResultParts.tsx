'use client'

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Sparkles, Users, MapPin, Clock } from "lucide-react";

const TOOL_LABELS: Record<string, string> = {
  searchStadiums: "Tìm kiếm sân đấu",
  getStadiumSlots: "Tra cứu khung giờ trống",
  findMatchRequests: "Tìm kèo ghép thể thao",
};

function getToolLabel(toolName: string) {
  return TOOL_LABELS[toolName] || "Xử lý yêu cầu";
}

function isToolPart(part: any): boolean {
  return part.type === "dynamic-tool" || (typeof part.type === "string" && part.type.startsWith("tool-"));
}

function getToolName(part: any): string {
  return part.type === "dynamic-tool" ? part.toolName : part.type.slice("tool-".length);
}

interface ToolResultPartsProps {
  parts: any[];
  /** Popup widget nhỏ (360px) — ép grid 1 cột thay vì 2 cột như trang /ai-assistant */
  compact?: boolean;
}

/**
 * Render trạng thái tool-call (đang xử lý / lỗi / card kết quả) cho message của assistant.
 * AI SDK v5 gõ mỗi tool part là `tool-${toolName}` (hoặc `dynamic-tool` khi tên tool không
 * biết trước ở client), field toolCallId/state/input/output nằm phẳng trên part — không lồng
 * trong toolCall/toolResult như v4. Dùng chung cho page.tsx và AIAssistantWidget.tsx để
 * tránh lặp lại 2 bộ card giống hệt nhau.
 */
export function ToolResultParts({ parts, compact = false }: ToolResultPartsProps) {
  if (!parts) return null;
  const gridCols = compact ? "grid-cols-1" : "grid-cols-1 sm:grid-cols-2";

  return (
    <>
      {parts.map((part: any, partIdx: number) => {
        if (!isToolPart(part)) {
          return null;
        }

        const toolName = getToolName(part);
        const toolCallId = part.toolCallId;
        const toolLabel = getToolLabel(toolName);

        if (part.state === 'input-streaming' || part.state === 'input-available') {
          return (
            <div key={toolCallId || partIdx} className="text-xs text-muted-foreground italic flex items-center gap-2 my-1">
              <span className="animate-spin h-3.5 w-3.5 border-2 border-primary border-t-transparent rounded-full" />
              Đang xử lý: {toolLabel}...
            </div>
          );
        }

        if (part.state === 'output-error') {
          return (
            <div key={toolCallId || partIdx} className="text-xs text-destructive italic my-1">
              Lỗi khi {toolLabel.toLowerCase()}: {part.errorText || 'không xác định'}
            </div>
          );
        }

        if (part.state === 'output-available') {
          const result = part.output;

          if (result && typeof result === 'object' && !Array.isArray(result) && result.error) {
            return (
              <div key={toolCallId || partIdx} className="text-xs text-destructive italic my-1">
                {String(result.error)}
              </div>
            );
          }

          if (toolName === 'searchStadiums' && Array.isArray(result)) {
            if (result.length === 0) {
              return (
                <div key={toolCallId || partIdx} className="text-xs text-yellow-600 italic my-1">
                  Không tìm thấy sân đấu nào phù hợp với yêu cầu.
                </div>
              );
            }
            return (
              <div key={toolCallId || partIdx} className={`grid gap-3 ${gridCols} mt-2`}>
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
                          onClick={() => window.open(`/venues/${stadium.stadiumId}`, '_blank', 'noopener,noreferrer')}
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

          if (toolName === 'findMatchRequests' && Array.isArray(result)) {
            if (result.length === 0) {
              return (
                <div key={toolCallId || partIdx} className="text-xs text-yellow-600 italic my-1">
                  Không tìm thấy kèo ghép nào đang mở phù hợp với yêu cầu.
                </div>
              );
            }
            return (
              <div key={toolCallId || partIdx} className={`grid gap-3 ${gridCols} mt-2`}>
                {result.map((match: any) => (
                  <Card key={match.matchId} className="border border-border/85 shadow-sm bg-card overflow-hidden transition-all hover:shadow-lg">
                    <CardContent className="p-3 space-y-2">
                      <div className="flex justify-between items-start gap-2">
                        <h4 className="font-bold text-xs leading-tight line-clamp-2">{match.title}</h4>
                        <Badge variant="secondary" className="bg-teal-500/10 text-teal-600 dark:text-teal-400 border-none px-1.5 py-0 text-[10px] shrink-0">
                          {match.sportName || 'Thể thao'}
                        </Badge>
                      </div>
                      <div className="space-y-1 text-[10px] text-muted-foreground">
                        <div className="flex items-center gap-1">
                          <MapPin className="h-3 w-3 shrink-0" />
                          <span className="line-clamp-1">{match.stadiumName || match.stadiumAddress || 'Chưa xác định sân'}</span>
                        </div>
                        <div className="flex items-center gap-1">
                          <Clock className="h-3 w-3 shrink-0" />
                          <span>{match.playDate} · {match.startTime}–{match.endTime}</span>
                        </div>
                        <div className="flex items-center gap-1">
                          <Users className="h-3 w-3 shrink-0" />
                          <span>{match.currentPlayers}/{match.maxPlayers} người</span>
                        </div>
                      </div>
                      <div className="border-t pt-2 flex items-center justify-between">
                        <span className="text-xs font-bold text-primary">
                          {match.pricePerPlayer ? `${match.pricePerPlayer.toLocaleString('vi-VN')}đ/người` : 'Miễn phí'}
                        </span>
                        <Button
                          size="sm"
                          className="bg-primary hover:bg-primary/95 text-[10px] h-7 px-3 rounded-full"
                          onClick={() => window.open(`/community?matchId=${match.matchId}`, '_blank', 'noopener,noreferrer')}
                        >
                          Xem kèo
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
              <span>Hoàn thành: {toolLabel}</span>
            </div>
          );
        }

        return null;
      })}
    </>
  );
}
