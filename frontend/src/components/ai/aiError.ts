/**
 * Chuyển error từ useChat thành thông điệp an toàn cho người dùng.
 *
 * Backend đã gửi errorText tiếng Việt phân loại sẵn (rate limit, timeout, cấu hình...) qua
 * SSE event "error" — AI SDK đưa nguyên văn vào error.message, hiển thị được luôn.
 * Nhưng lỗi tầng transport (fetch fail, HTTP status, parse...) là chuỗi kỹ thuật tiếng Anh
 * ("Failed to fetch", "Unexpected token...") — không được lộ ra UI.
 * Heuristic: message có dấu tiếng Việt => là message chủ đích từ backend => hiển thị.
 */
const VIETNAMESE_CHAR_PATTERN = /[\u00C0-\u1EF9]/;

export function friendlyAiError(error: Error | undefined): string {
  const fallback = 'Xin lỗi, trợ lý AI đang gặp sự cố kết nối. Bạn vui lòng thử lại sau nhé.';
  if (!error?.message) {
    return fallback;
  }
  return VIETNAMESE_CHAR_PATTERN.test(error.message) ? error.message : fallback;
}
