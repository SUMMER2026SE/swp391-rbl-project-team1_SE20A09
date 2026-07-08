package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportvenue.dto.response.AiChatTurnResponse;
import org.springframework.stereotype.Component;

/**
 * Xử lý intent "get_policy" — port từ CustomerAgentToolProvider.handleGetPolicyInformation
 * (nhánh ai-chatting cũ).
 *
 * QUAN TRỌNG: nội dung trả về ở đây PHẢI khớp đúng với prompts/customer/faq.md (nguồn chính
 * thức duy nhất) — không thêm số liệu/mốc thời gian/chi tiết nào ngoài file đó. Bản cũ từng bịa
 * "hủy trước 24 tiếng, mất cọc" và "hoàn tiền trong 3-5 ngày" — những con số này KHÔNG tồn tại
 * trong faq.md lẫn logic thật, khiến AI nói sai chính sách cho khách.
 */
@Component
public class PolicyHandler {

    public AiChatTurnResponse handle(JsonNode args, String llmMessage) {
        if (!args.hasNonNull("topic")) {
            return AiChatTurnResponse.builder()
                    .message("Bạn muốn hỏi về chính sách gì cụ thể — thanh toán, hủy sân hay hoàn tiền?")
                    .intent("get_policy")
                    .build();
        }
        String topic = args.get("topic").asText().toLowerCase();
        String policy;
        if (topic.contains("vnpay") || topic.contains("thanh toán") || topic.contains("payment")) {
            policy = "Hỗ trợ thanh toán qua VNPay, MoMo, chuyển khoản ngân hàng và tiền mặt tại sân. Mỗi đơn có phí dịch vụ 20.000đ. Sau khi xác nhận đặt sân, đơn được giữ chỗ 5 phút để thanh toán; quá 5 phút chưa thanh toán, đơn tự hủy và slot được trả lại.";
        } else if (topic.contains("cancel") || topic.contains("hủy")) {
            policy = "Vào mục 'Đơn đặt sân của tôi' trên website, chọn đơn và bấm Hủy kèm lý do. Chỉ hủy được đơn CHƯA hoàn thành và CHƯA bị hủy trước đó. Nếu đơn đã thanh toán, hệ thống ghi nhận hoàn tiền; tiền hoàn về qua kênh thanh toán ban đầu và có thể cần thời gian xử lý.";
        } else if (topic.contains("refund") || topic.contains("hoàn tiền")) {
            policy = "Hoàn tiền được ghi nhận khi bạn hủy một đơn đã thanh toán hợp lệ (xem chính sách hủy đặt sân) — tiền về qua đúng kênh thanh toán ban đầu và có thể cần thời gian xử lý.";
        } else {
            policy = "Không tìm thấy chính sách cụ thể cho chủ đề này. Vui lòng liên hệ bộ phận CSKH để biết thêm chi tiết.";
        }

        return AiChatTurnResponse.builder()
                .message(llmMessage)
                .intent("get_policy")
                .policyText(policy)
                .build();
    }
}
