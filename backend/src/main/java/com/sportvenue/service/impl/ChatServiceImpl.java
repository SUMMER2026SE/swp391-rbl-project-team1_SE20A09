package com.sportvenue.service.impl;

import com.sportvenue.dto.chat.ChatMessageDto;
import com.sportvenue.dto.chat.ChatbotRequest;
import com.sportvenue.dto.chat.ChatbotResponse;
import com.sportvenue.dto.chat.ConversationDto;
import com.sportvenue.dto.chat.SendMessageRequest;
import com.sportvenue.entity.ChatConversation;
import com.sportvenue.entity.ChatMessage;
import com.sportvenue.entity.ChatbotLog;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.MessageType;
import com.sportvenue.repository.ChatConversationRepository;
import com.sportvenue.repository.ChatMessageRepository;
import com.sportvenue.repository.ChatbotLogRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;
    private final ChatbotLogRepository chatbotLogRepo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(Integer userId) {
        List<ChatConversation> conversations = conversationRepo.findAllByUserId(userId);
        return conversations.stream()
                .map(conv -> toConversationDto(conv, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getMessages(Long conversationId, Integer userId, Pageable pageable) {
        ChatConversation conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        // Verify the user is a participant
        boolean isParticipant = false;
        if (Boolean.TRUE.equals(conv.getIsGroup())) {
            isParticipant = conv.getParticipants().stream().anyMatch(u -> u.getUserId().equals(userId));
        } else {
            isParticipant = (conv.getUser1() != null && conv.getUser1().getUserId().equals(userId)) ||
                            (conv.getUser2() != null && conv.getUser2().getUserId().equals(userId));
        }

        if (!isParticipant) {
            throw new SecurityException("You are not a participant of this conversation");
        }

        return messageRepo.findByConversationId(conversationId, pageable)
                .map(this::toMessageDto);
    }

    @Override
    @Transactional
    public ChatMessageDto sendMessage(Integer senderId, SendMessageRequest request) {
        User sender = userRepo.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        ChatConversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepo.findById(request.getConversationId())
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        } else {
            if (request.getRecipientId() == null) {
                throw new IllegalArgumentException("Recipient ID is required for 1-on-1 chat");
            }
            User recipient = userRepo.findById(request.getRecipientId())
                    .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
            
            if (senderId.equals(request.getRecipientId())) {
                throw new IllegalArgumentException("Cannot send message to yourself");
            }
            
            Integer u1 = Math.min(senderId, request.getRecipientId());
            Integer u2 = Math.max(senderId, request.getRecipientId());

            conversation = conversationRepo.findByUserPair(u1, u2)
                    .orElseGet(() -> {
                        User user1 = u1.equals(senderId) ? sender : recipient;
                        User user2 = u2.equals(senderId) ? sender : recipient;
                        ChatConversation newConv = ChatConversation.builder()
                                .user1(user1)
                                .user2(user2)
                                .isGroup(false)
                                .build();
                        return conversationRepo.save(newConv);
                    });
        }

        // Determine message type
        MessageType type = MessageType.TEXT;
        if (request.getMessageType() != null) {
            try {
                type = MessageType.valueOf(request.getMessageType().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown message type '{}', defaulting to TEXT", request.getMessageType());
            }
        }

        // Create and save message
        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .messageType(type)
                .sentAt(now)
                .build();
        message = messageRepo.save(message);

        // Update conversation preview
        String preview = request.getContent();
        if (preview.length() > 100) {
            preview = preview.substring(0, 100) + "...";
        }
        conversation.setLastMessagePreview(preview);
        conversation.setLastMessageAt(now);
        conversationRepo.save(conversation);

        log.info("Message sent from user {} to user {} in conversation {}",
                senderId, request.getRecipientId(), conversation.getConversationId());

        return toMessageDto(message);
    }

    @Override
    @Transactional
    public void markAsRead(Long conversationId, Integer userId) {
        messageRepo.markAllAsRead(conversationId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Integer userId) {
        return messageRepo.countUnreadForUser(userId);
    }

    @Override
    @Transactional
    public ChatbotResponse processChatbotQuery(Integer userId, ChatbotRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // AI response logic — smart mock for MVP
        String reply = generateSmartReply(request.getMessage());

        // Persist chatbot log
        ChatbotLog logEntry = ChatbotLog.builder()
                .user(user)
                .userMessage(request.getMessage())
                .botResponse(reply)
                .build();
        chatbotLogRepo.save(logEntry);

        return ChatbotResponse.builder()
                .reply(reply)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public Long getOrCreateConversation(Integer userId1, Integer userId2) {
        Integer u1 = Math.min(userId1, userId2);
        Integer u2 = Math.max(userId1, userId2);

        return conversationRepo.findByUserPair(u1, u2)
                .map(ChatConversation::getConversationId)
                .orElseGet(() -> {
                    User user1 = userRepo.findById(u1)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + u1));
                    User user2 = userRepo.findById(u2)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + u2));
                    ChatConversation conv = ChatConversation.builder()
                            .user1(user1)
                            .user2(user2)
                            .build();
                    return conversationRepo.save(conv).getConversationId();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> searchUsers(Integer currentUserId, String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        List<User> users = userRepo.searchByName(query.trim());
        return users.stream()
                .filter(u -> !u.getUserId().equals(currentUserId))
                .limit(20)
                .map(u -> ConversationDto.builder()
                        .otherUserId(u.getUserId())
                        .otherUserName(u.getFullName())
                        .otherUserAvatar(u.getAvatarUrl())
                        .otherUserOnline(false)
                        .unreadCount(0)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createOrUpdateMatchGroupChat(com.sportvenue.entity.MatchRequest match, Integer newUserId) {
        // Find existing match group chat or create
        ChatConversation group = conversationRepo.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsGroup()) && c.getMatch() != null && c.getMatch().getMatchId().equals(match.getMatchId()))
                .findFirst()
                .orElseGet(() -> {
                    String groupName = match.getSportType().getSportName() + " " + match.getPlayDate().toString() + " - Kèo " + match.getUser().getFirstName();
                    ChatConversation newGroup = ChatConversation.builder()
                            .isGroup(true)
                            .name(groupName)
                            .match(match)
                            .build();
                    newGroup.getParticipants().add(match.getUser()); // Add host
                    return conversationRepo.save(newGroup);
                });
        
        User newUser = userRepo.findById(newUserId).orElseThrow();
        group.getParticipants().add(newUser);
        
        LocalDateTime now = LocalDateTime.now();
        String systemMessage = "✅ " + newUser.getFullName() + " đã tham gia kèo.";
        
        // System message
        ChatMessage msg = ChatMessage.builder()
                .conversation(group)
                .sender(match.getUser()) // Can be system user ideally, but host is fine
                .content(systemMessage)
                .messageType(MessageType.SYSTEM)
                .sentAt(now)
                .build();
        messageRepo.save(msg);
        
        group.setLastMessagePreview(systemMessage);
        group.setLastMessageAt(now);
        conversationRepo.save(group);
        
        ConversationDto payload = toConversationDto(group, null);
        for (User p : group.getParticipants()) {
            messagingTemplate.convertAndSend("/topic/chat/user/" + p.getUserId() + "/new-group", payload);
        }
    }

    // ── Private helpers ──────────────────────────────────────────

    private ConversationDto toConversationDto(ChatConversation conv, Integer currentUserId) {
        long unread = messageRepo.countUnreadInConversation(
                conv.getConversationId(), currentUserId);

        if (Boolean.TRUE.equals(conv.getIsGroup())) {
            return ConversationDto.builder()
                    .conversationId(conv.getConversationId())
                    .isGroup(true)
                    .otherUserId(null)
                    .otherUserName(conv.getName() != null ? conv.getName() : "Group Chat")
                    .otherUserAvatar(null)
                    .otherUserOnline(false)
                    .lastMessagePreview(conv.getLastMessagePreview())
                    .lastMessageAt(conv.getLastMessageAt())
                    .unreadCount(unread)
                    .build();
        } else {
            User otherUser = (conv.getUser1() != null && conv.getUser1().getUserId().equals(currentUserId))
                    ? conv.getUser2()
                    : conv.getUser1();

            return ConversationDto.builder()
                    .conversationId(conv.getConversationId())
                    .isGroup(false)
                    .otherUserId(otherUser.getUserId())
                    .otherUserName(otherUser.getFullName())
                    .otherUserAvatar(otherUser.getAvatarUrl())
                    .otherUserOnline(false) // Will be updated by WebSocket presence
                    .lastMessagePreview(conv.getLastMessagePreview())
                    .lastMessageAt(conv.getLastMessageAt())
                    .unreadCount(unread)
                    .build();
        }
    }

    private ChatMessageDto toMessageDto(ChatMessage msg) {
        User sender = msg.getSender();
        return ChatMessageDto.builder()
                .messageId(msg.getMessageId())
                .conversationId(msg.getConversation().getConversationId())
                .senderId(sender.getUserId())
                .senderName(sender.getFullName())
                .senderAvatar(sender.getAvatarUrl())
                .content(msg.getContent())
                .messageType(msg.getMessageType().name())
                .isRead(msg.getIsRead())
                .sentAt(msg.getSentAt())
                .build();
    }

    /**
     * Smart mock chatbot — responds contextually to common SportVenue queries.
     * Replace with real AI API (Claude/GPT) in production by adding API key config.
     */
    private String generateSmartReply(String userMessage) {
        String lower = userMessage.toLowerCase();

        if (lower.contains("đặt sân") || lower.contains("booking") || lower.contains("book")) {
            return "🏟️ Để đặt sân, bạn có thể:\n"
                    + "1. Vào trang \"Tìm sân\" để xem danh sách sân\n"
                    + "2. Chọn sân phù hợp và xem lịch trống\n"
                    + "3. Chọn khung giờ và nhấn \"Đặt sân\"\n"
                    + "4. Thanh toán qua VNPay hoặc tiền mặt\n\n"
                    + "Bạn cần hỗ trợ cụ thể hơn không?";
        }

        if (lower.contains("giá") || lower.contains("price") || lower.contains("chi phí") || lower.contains("phí")) {
            return "💰 Giá thuê sân phụ thuộc vào:\n"
                    + "• Loại sân (bóng đá, cầu lông, bóng rổ...)\n"
                    + "• Khung giờ (giờ cao điểm / thấp điểm)\n"
                    + "• Vị trí sân\n\n"
                    + "Bạn có thể xem giá chi tiết tại trang thông tin từng sân. "
                    + "Trung bình khoảng 200.000 - 500.000 VNĐ/giờ.";
        }

        if (lower.contains("hủy") || lower.contains("cancel") || lower.contains("hoàn")) {
            return "❌ Chính sách hủy đặt sân:\n"
                    + "• Hủy trước 24 giờ: hoàn 100% tiền\n"
                    + "• Hủy trước 12 giờ: hoàn 50% tiền\n"
                    + "• Hủy dưới 12 giờ: không hoàn tiền\n\n"
                    + "Để hủy, vào \"Lịch sử đặt sân\" → chọn booking → nhấn \"Hủy\".";
        }

        if (lower.contains("thanh toán") || lower.contains("payment") || lower.contains("vnpay")) {
            return "💳 Chúng tôi hỗ trợ các phương thức thanh toán:\n"
                    + "• VNPay (thẻ ATM, Visa, MasterCard)\n"
                    + "• Chuyển khoản ngân hàng\n"
                    + "• Tiền mặt tại sân\n\n"
                    + "Thanh toán online được xác nhận tự động trong vài phút.";
        }

        if (lower.contains("ghép kèo") || lower.contains("tìm đối") || lower.contains("matchmaking")) {
            return "⚽ Tính năng Ghép Kèo giúp bạn:\n"
                    + "• Tìm người chơi cùng trình độ\n"
                    + "• Ghép đội cho trận đấu\n"
                    + "• Chia sẻ chi phí sân\n\n"
                    + "Vào mục \"Cộng đồng\" → \"Tạo kèo mới\" để bắt đầu!";
        }

        if (lower.contains("đánh giá") || lower.contains("review") || lower.contains("feedback")) {
            return "⭐ Bạn có thể đánh giá sân sau khi hoàn thành booking:\n"
                    + "• Vào \"Lịch sử đặt sân\"\n"
                    + "• Chọn booking đã hoàn thành\n"
                    + "• Viết đánh giá và cho điểm (1-5 sao)\n\n"
                    + "Đánh giá giúp cộng đồng lựa chọn sân tốt hơn!";
        }

        if (lower.contains("xin chào") || lower.contains("hello") || lower.contains("hi") || lower.contains("chào")) {
            return "👋 Xin chào! Tôi là trợ lý AI của SportHub.\n\n"
                    + "Tôi có thể giúp bạn:\n"
                    + "• Tìm và đặt sân thể thao\n"
                    + "• Giải đáp thắc mắc về thanh toán\n"
                    + "• Hướng dẫn sử dụng hệ thống\n"
                    + "• Tư vấn ghép kèo chơi thể thao\n\n"
                    + "Bạn cần hỗ trợ gì?";
        }

        if (lower.contains("liên hệ") || lower.contains("hotline") || lower.contains("support")) {
            return "📞 Thông tin liên hệ:\n"
                    + "• Hotline: 1900 xxxx (8:00 - 22:00)\n"
                    + "• Email: support@sporthub.vn\n"
                    + "• Chat trực tiếp với đội ngũ hỗ trợ qua hệ thống này\n\n"
                    + "Hoặc bạn có thể mô tả vấn đề, tôi sẽ cố gắng giúp!";
        }

        // Default response
        return "🤖 Cảm ơn bạn đã liên hệ! Tôi là trợ lý AI của SportHub.\n\n"
                + "Tôi có thể giúp bạn với:\n"
                + "• 🏟️ Đặt sân thể thao\n"
                + "• 💰 Thông tin giá cả\n"
                + "• ❌ Hủy / hoàn tiền\n"
                + "• 💳 Thanh toán\n"
                + "• ⚽ Ghép kèo\n"
                + "• ⭐ Đánh giá sân\n\n"
                + "Hãy hỏi tôi bất cứ điều gì bạn cần!";
    }
}
