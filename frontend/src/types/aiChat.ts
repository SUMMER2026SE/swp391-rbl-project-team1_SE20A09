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

export interface AiChatTurnResponse {
  message: string;
  intent: string;
  stadiums: StadiumResponse[] | null;
  slots: TimeSlotResponse[] | null;
  matches: MatchResponse[] | null;
  policyText: string | null;
}
