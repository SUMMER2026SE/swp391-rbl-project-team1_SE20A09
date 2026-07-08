import { post } from "./api";
import { AiChatTurnRequest, AiChatTurnResponse, ChatMessage } from "@/types/aiChat";
import { ApiResponse } from "@/types/common";

const SESSION_KEY = "ai_session_id";

function getOrCreateSessionId(): string {
  if (typeof window === "undefined") {
    return "";
  }
  let sessionId = sessionStorage.getItem(SESSION_KEY);
  if (!sessionId) {
    sessionId = crypto.randomUUID();
    sessionStorage.setItem(SESSION_KEY, sessionId);
  }
  return sessionId;
}

export async function sendChatMessage(message: string, history: ChatMessage[]): Promise<AiChatTurnResponse> {
  const sessionId = getOrCreateSessionId();
  const payload: AiChatTurnRequest = { message, history };
  
  const response = await post<ApiResponse<AiChatTurnResponse>>("/ai/chat", payload, {
    headers: {
      "X-Session-ID": sessionId
    }
  });

  return response.result;
}
