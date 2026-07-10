import { StadiumResponse } from "./stadium";
import { MatchResponse } from "./match";

export interface ChatMessage {
  role: "user" | "assistant";
  content: string;
}

export interface AiChatTurnRequest {
  message: string;
  history: ChatMessage[];
}

export interface TimeSlotResponse {
  slotId: number;
  stadiumId: number;
  startTime: string; // HH:mm:ss
  endTime: string; // HH:mm:ss
  pricePerSlot: number;
  slotStatus: string;
  available: boolean;
}

export interface DraftBookingResponse {
  stadiumId: number;
  stadiumName: string;
  date: string; // YYYY-MM-DD
  startTime: string; // HH:mm
  price: number;
}

export interface DraftJoinMatchResponse {
  matchId: number;
  title: string;
  stadiumName: string;
  playDate: string;
  time: string;
  userMessage: string;
}

export interface AiChatTurnResponse {
  message: string;
  intent: string;
  stadiums: StadiumResponse[] | null;
  slots: TimeSlotResponse[] | null;
  matches: MatchResponse[] | null;
  policyText: string | null;
  /** ID của booking vừa tạo (intent: create_booking) - deprecated */
  bookingId?: number | null;
  /** Draft booking info for user confirmation (intent: confirm_booking) */
  draftBooking?: DraftBookingResponse | null;
  /** ID của kèo ghép vừa tham gia (intent: join_match) */
  matchId?: number | null;
  /** Thông tin kèo ghép nháp (intent: confirm_join_match) */
  draftJoinMatch?: DraftJoinMatchResponse | null;
}
