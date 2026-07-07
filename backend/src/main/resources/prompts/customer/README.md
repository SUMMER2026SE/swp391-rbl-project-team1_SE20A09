# Customer Agent — Prompt Files

Áp dụng `ai-patterns/1-prompt-engineering` vào `CustomerAgentToolProvider`: mỗi role có
1 file LLM đọc (`*.md`, nội dung nạp thẳng làm system prompt — KHÔNG có comment, vì
comment sẽ lẫn vào prompt gửi cho Groq) + 1 file người đọc (file này — không được nạp
vào code, chỉ để giải thích lý do từng luật tồn tại).

## File nào làm gì

| File | Vai trò | Nạp vào code? |
|---|---|---|
| `system-prompt.md` | Luật hành vi cốt lõi của agent Customer | Có — `CustomerAgentToolProvider.CUSTOMER_SYSTEM_PROMPT` |
| `faq.md` | Nghiệp vụ đặt sân/thanh toán/hủy — chống bịa chính sách | Có — `CustomerAgentToolProvider.FAQ_PROMPT` |
| `guest-suffix.md` | Chỉ thêm vào khi user CHƯA đăng nhập | Có — `CustomerAgentToolProvider.GUEST_SYSTEM_PROMPT_SUFFIX` |
| `logged-in-suffix.md` | Chỉ thêm vào khi user ĐÃ đăng nhập | Có — `CustomerAgentToolProvider.LOGGED_IN_SYSTEM_PROMPT_SUFFIX` |
| `README.md` (file này) | Rationale/bug ref cho từng luật | Không |

`getSystemPrompt(userId)` ghép 4 phần này + `buildCurrentTimeContext()` (ngày giờ VN
hiện tại — buộc phải tính động trong Java, không thể để trong file tĩnh) bằng
`String.join(" ", ...)`.

## Vì sao tách ra file thay vì để hardcode trong Java (trước đây)

- Sửa nội dung prompt (câu chữ, thêm luật mới) không cần recompile/động vào code Java — review diff trên PR cũng sạch hơn (diff đúng câu chữ, không lẫn cú pháp `"..." +`).
- `AgentRegistry` đã chuẩn bị sẵn cho `OwnerAgentToolProvider`/`AdminAgentToolProvider` sau này (xem comment trong `AgentRegistry.java`) — mẫu `prompts/<role>/*.md` này dùng lại được nguyên cấu trúc, chỉ đổi nội dung.
- Nội dung prompt là thứ cả người ít kinh nghiệm Java cũng sửa được an toàn (không đụng logic), tách bạch rõ "ai sửa câu chữ" (ai cũng sửa được) và "ai sửa logic" (cần hiểu Java).

## Rationale từng luật trong `system-prompt.md` (map theo bug số trong `ai-chatting-test-guide.md`)

- Đoạn 1 (mở đầu, vai trò): giới thiệu vai trò + ngôn ngữ trả lời.
- Đoạn 2 (đồng bộ card UI — bug #2): danh sách sân/kèo đã render thành card ở FE; nếu AI tự liệt kê lại trong text sẽ lệch với card hiển thị. Câu "Dưới đây là..." chỉ dùng khi CÓ kết quả — tránh lỗi từng gặp: AI nói "có sân phù hợp" trong khi card báo rỗng.
- Đoạn 3 (không bịa stadiumId): model từng tự gọi `getStadiumSlots` với `stadiumId=0` khi search rỗng — chặn thẳng ở đây.
- Đoạn 4 (không đoán tham số — bug #8): câu hỏi mơ hồ như "có sân nào trống không" từng khiến model tự đoán môn/khu vực bừa.
- Đoạn 5 (hỏi lại vị trí — bug #3, ngắn hạn): "gần nhất"/"gần đây" chưa có tọa độ thật, phải hỏi lại trước khi search.
- Đoạn 6 (không gợi ý sân bảo trì/đóng cửa + luật available của `getStadiumSlots`): tool trả `available: true/false` theo slot, model từng gợi ý cả slot đã đặy kín.
- Đoạn 7 (báo lỗi lịch sự): fallback khi tool lỗi.
- Đoạn 8 (giới hạn phạm vi SportHub): chặn out-of-scope request (code, toán, chat phiếm).
- Đoạn 9 (chống prompt injection / lộ system prompt): chặn "ignore previous instructions", roleplay, dò dữ liệu người dùng khác.
- Đoạn 10 (escalation): câu thoại thoát khi bot bí — hotline/Zalo CSKH.

## Lưu ý khi sửa

- KHÔNG thêm markdown syntax "nặng" (bảng, heading `#`, code block) vào các file LLM đọc (`system-prompt.md`, `faq.md`, `guest-suffix.md`, `logged-in-suffix.md`) — model coi cả file là 1 đoạn system message thô, ký tự markdown thừa chỉ tốn token không có lợi. Đoạn văn xuống dòng (blank line) là an toàn, đã dùng sẵn.
- Sau khi sửa, chạy `mvn test -Dtest=CustomerAgentToolProviderTest` — vài test check `.contains("...")` trên vài cụm từ cố định (vd `"GIỮ CHỖ 5 PHÚT"`, `"khách vãng lai"`) — xóa/đổi các cụm đó cần sửa test tương ứng.
- Sau khi sửa xong, nên chạy `python ai-eval/run_eval.py` (pattern 2) để chấm lại toàn bộ hành vi thật qua Groq, không chỉ check string tồn tại như unit test.
