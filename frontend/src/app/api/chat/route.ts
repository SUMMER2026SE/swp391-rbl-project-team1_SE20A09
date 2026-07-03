import { createGroq } from '@ai-sdk/groq';
import { streamText, convertToModelMessages, toUIMessageStream, createUIMessageStreamResponse } from 'ai';

// Allow streaming responses up to 30 seconds
export const maxDuration = 30;

export async function POST(req: Request) {
  try {
    const { messages } = await req.json();
    const apiKey = process.env.GROQ_API_KEY;

    if (!apiKey) {
      const stream = new ReadableStream({
        async start(controller) {
          controller.enqueue({ type: 'text-start', id: 'warning' });
          controller.enqueue({
            type: 'text-delta',
            delta: "⚠️ Cảnh báo: `GROQ_API_KEY` chưa được cấu hình. Vui lòng thêm `GROQ_API_KEY` vào file `frontend/.env.local` để bắt đầu trò chuyện với trợ lý ảo AI của SportHub.",
            id: 'warning'
          });
          controller.enqueue({ type: 'text-end', id: 'warning' });
          controller.close();
        },
      });

      return createUIMessageStreamResponse({ stream });
    }

    const groq = createGroq({ apiKey });

    const result = await streamText({
      model: groq('llama-3.3-70b-versatile'),
      messages: await convertToModelMessages(messages),
      system: `Bạn là trợ lý ảo chính thức của SportHub - nền tảng đặt sân thể thao trực tuyến hàng đầu.
Nhiệm vụ của bạn là:
1. Hỗ trợ người dùng tìm kiếm sân thể thao phù hợp (bóng đá, cầu lông, tennis, bóng rổ, v.v.).
2. Giải đáp các thắc mắc về luật chơi, hướng dẫn sử dụng website, quy trình đặt sân, thanh toán qua VNPay, chính sách hủy sân, hoàn tiền và phản hồi các khiếu nại.
3. Giải đáp về các gói ưu đãi, khung giờ hoạt động tiêu chuẩn và dịch vụ đi kèm (thuê phụ kiện, nước uống).

Quy tắc ứng xử và phong cách trả lời:
- Luôn thân thiện, lịch sự, chuyên nghiệp và phản hồi bằng Tiếng Việt.
- Trả lời ngắn gọn, tập trung đúng vào câu hỏi, trình bày rõ ràng bằng Markdown (dùng bullet points, in đậm khi cần thiết).
- Tránh trả lời lan man hoặc giả định thông tin không có thật. Nếu không biết hoặc thông tin quá đặc thù của sân, hãy gợi ý người dùng liên hệ với chủ sân hoặc hotline hỗ trợ.`,
    });

    const stream = toUIMessageStream({ stream: result.fullStream });
    return createUIMessageStreamResponse({ stream });
  } catch (error) {
    console.error("Error in chat API route:", error);
    return new Response(
      JSON.stringify({ error: "Internal Server Error" }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    );
  }
}
