package com.sportvenue.service.impl;

import com.sportvenue.dto.chat.ChatMessageDto;
import com.sportvenue.dto.chat.BlockStatusDto;
import com.sportvenue.dto.chat.ChatbotRequest;
import com.sportvenue.dto.chat.ChatbotResponse;
import com.sportvenue.dto.chat.ConversationDto;
import com.sportvenue.dto.chat.SendMessageRequest;
import com.sportvenue.entity.ChatConversation;
import com.sportvenue.entity.ChatConversationHiddenState;
import com.sportvenue.entity.ChatMessage;
import com.sportvenue.entity.ChatbotLog;
import com.sportvenue.entity.JoinRequest;
import com.sportvenue.entity.MatchRequest;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.JoinRequestStatus;
import com.sportvenue.entity.enums.MessageType;
import com.sportvenue.repository.ChatConversationRepository;
import com.sportvenue.repository.ChatMessageRepository;
import com.sportvenue.repository.ChatbotLogRepository;
import com.sportvenue.repository.ChatConversationHiddenStateRepository;
import com.sportvenue.repository.JoinRequestRepository;
import com.sportvenue.repository.MatchRequestRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.ChatService;
import com.sportvenue.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
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
    private final ChatConversationHiddenStateRepository hiddenStateRepo;
    private final MatchRequestRepository matchRequestRepo;
    private final JoinRequestRepository joinRequestRepo;
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

        return messageRepo.findByConversationId(conversationId, userId, pageable)
                .map(this::toMessageDto);
    }

    @Override
    @Transactional
    public ChatMessageDto sendMessage(Integer senderId, SendMessageRequest request) {
        User sender = userRepo.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        ChatConversation conversation = resolveConversation(senderId, sender, request);

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

        // Update conversation preview for non-system messages
        if (type != MessageType.SYSTEM) {
            String preview = request.getContent();
            if (preview.length() > 100) {
                preview = preview.substring(0, 100) + "...";
            }
            conversation.setLastMessagePreview(preview);
            conversation.setLastMessageAt(now);
            conversationRepo.save(conversation);
        }

        log.info("Message sent from user {} to conversation {}",
                senderId, conversation.getConversationId());

        ChatMessageDto messageDto = toMessageDto(message);

        // Broadcast to all participants
        if (Boolean.TRUE.equals(conversation.getIsGroup())) {
            for (User participant : conversation.getParticipants()) {
                messagingTemplate.convertAndSend("/topic/chat/user/" + participant.getUserId(), messageDto);
            }
        } else {
            messagingTemplate.convertAndSend("/topic/chat/user/" + conversation.getUser1().getUserId(), messageDto);
            messagingTemplate.convertAndSend("/topic/chat/user/" + conversation.getUser2().getUserId(), messageDto);
        }

        return messageDto;
    }

    private ChatConversation resolveConversation(Integer senderId, User sender, SendMessageRequest request) {
        if (request.getConversationId() != null) {
            ChatConversation conversation = conversationRepo.findById(request.getConversationId())
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

            // BLOCK VALIDATION: Reject messages in blocked conversations
            if (!Boolean.TRUE.equals(conversation.getIsGroup())
                    && (Boolean.TRUE.equals(conversation.getIsMutualBlock()) || conversation.getBlockedBy() != null)) {
                throw new IllegalArgumentException("USER_BLOCKED: Cannot send message in a blocked conversation");
            }

            // MEMBERSHIP VALIDATION: Ensure user is still in the group
            if (Boolean.TRUE.equals(conversation.getIsGroup())) {
                boolean isParticipant = conversation.getParticipants().stream()
                        .anyMatch(u -> u.getUserId().equals(senderId));
                if (!isParticipant) {
                    throw new SecurityException("You are no longer a participant of this group");
                }
            }
            return conversation;
        }

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

        ChatConversation conversation = conversationRepo.findByUserPair(u1, u2)
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

        // BLOCK VALIDATION for recipient-based messages
        if (Boolean.TRUE.equals(conversation.getIsMutualBlock()) || conversation.getBlockedBy() != null) {
            throw new IllegalArgumentException("USER_BLOCKED: Cannot send message in a blocked conversation");
        }
        return conversation;
    }

    @Override
    @Transactional
    public void markAsRead(Long conversationId, Integer userId) {
        ChatConversation conv = conversationRepo.findById(conversationId).orElse(null);
        if (conv == null) {
            return;
        }

        User user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        boolean didUpdate = false;
        if (Boolean.TRUE.equals(conv.getIsGroup())) {
            List<ChatMessage> unreadMessages = messageRepo.findUnreadMessagesInConversation(conversationId, userId);
            for (ChatMessage msg : unreadMessages) {
                msg.getReadBy().add(user);
                didUpdate = true;
            }
            messageRepo.saveAll(unreadMessages);
        } else {
            // Check if there are actually unread messages before broadcasting
            if (messageRepo.countUnreadForUser(userId) > 0) {
                messageRepo.markAllAsRead(conversationId, userId);
                didUpdate = true;
            }
        }

        if (didUpdate) {
            String readPayload = "{\"type\":\"message_read\",\"conversationId\":" + conversationId
                    + ",\"readByUserId\":" + userId + ",\"readByUserAvatar\":\""
                    + (user.getAvatarUrl() != null ? user.getAvatarUrl() : "") + "\"}";

            if (Boolean.TRUE.equals(conv.getIsGroup())) {
                for (User participant : conv.getParticipants()) {
                    messagingTemplate.convertAndSend("/topic/chat/user/" + participant.getUserId(), readPayload);
                }
            } else {
                messagingTemplate.convertAndSend("/topic/chat/user/" + conv.getUser1().getUserId(), readPayload);
                messagingTemplate.convertAndSend("/topic/chat/user/" + conv.getUser2().getUserId(), readPayload);
            }
        }
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
    public void createOrUpdateMatchGroupChat(MatchRequest match, Integer newUserId) {
        // Find existing match group chat or create
        ChatConversation group = conversationRepo.findByMatchId(match.getMatchId())
                .orElseGet(() -> {
                    String groupName = match.getSportType().getSportName() + " " + match.getPlayDate().toString()
                            + " - Kèo " + match.getUser().getFirstName();
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

        ChatMessageDto msgDto = toMessageDto(msg);

        for (User p : group.getParticipants()) {
            ConversationDto payload = toConversationDto(group, p.getUserId());
            messagingTemplate.convertAndSend("/topic/chat/user/" + p.getUserId() + "/new-group", payload);
            messagingTemplate.convertAndSend("/topic/chat/user/" + p.getUserId(), msgDto);
        }
    }

    @Override
    @Transactional
    public Long getOrCreateMatchGroupChat(Integer matchId, Integer currentUserId) {
        MatchRequest match = matchRequestRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("KhÃ´ng tÃ¬m tháº¥y kÃ¨o ghÃ©p ID: " + matchId));

        ChatConversation group = conversationRepo.findByMatchId(matchId)
                .orElseGet(() -> createMatchGroupChatWithApprovedParticipants(match));

        boolean isMember = group.getParticipants().stream()
                .anyMatch(user -> user.getUserId().equals(currentUserId));
        if (!isMember) {
            throw new AccessDeniedException("Báº¡n chÆ°a pháº£i lÃ  thÃ nh viÃªn cá»§a nhÃ³m chat nÃ y");
        }

        return group.getConversationId();
    }

    private ChatConversation createMatchGroupChatWithApprovedParticipants(MatchRequest match) {
        String groupName = match.getSportType().getSportName() + " " + match.getPlayDate()
                + " - KÃ¨o " + match.getUser().getFirstName();
        ChatConversation newGroup = ChatConversation.builder()
                .isGroup(true)
                .name(groupName)
                .match(match)
                .build();

        newGroup.getParticipants().add(match.getUser());
        List<JoinRequest> approvedRequests = joinRequestRepo.findAllByMatchRequestMatchId(match.getMatchId());
        for (JoinRequest request : approvedRequests) {
            if (request.getRequestStatus() == JoinRequestStatus.APPROVED) {
                newGroup.getParticipants().add(request.getUser());
            }
        }

        ChatConversation saved = conversationRepo.save(newGroup);
        LocalDateTime now = LocalDateTime.now();
        String systemMessage = "âœ… NhÃ³m chat cá»§a kÃ¨o Ä‘Ã£ Ä‘Æ°á»£c khá»Ÿi táº¡o.";
        ChatMessage message = ChatMessage.builder()
                .conversation(saved)
                .sender(match.getUser())
                .content(systemMessage)
                .messageType(MessageType.SYSTEM)
                .sentAt(now)
                .build();
        messageRepo.save(message);

        saved.setLastMessagePreview(systemMessage);
        saved.setLastMessageAt(now);
        return conversationRepo.save(saved);
    }

    @Override
    @Transactional
    public ConversationDto renameGroupChat(Long conversationId, Integer userId, String newName) {
        ChatConversation conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (Boolean.TRUE.equals(conv.getIsGroup())) {
            boolean isParticipant = conv.getParticipants().stream()
                    .anyMatch(u -> u.getUserId().equals(userId));
            if (!isParticipant) {
                throw new SecurityException("You are not a participant of this group");
            }
            conv.setName(newName.trim());
            conversationRepo.save(conv);
            log.info("Group chat {} renamed to '{}' by user {}", conversationId, newName, userId);

            // Broadcast the rename to all participants
            for (User p : conv.getParticipants()) {
                ConversationDto payload = toConversationDto(conv, p.getUserId());
                messagingTemplate.convertAndSend("/topic/chat/user/" + p.getUserId() + "/group-renamed", payload);
            }
        } else {
            // Private chat: set nickname
            boolean isUser1 = conv.getUser1() != null && conv.getUser1().getUserId().equals(userId);
            boolean isUser2 = conv.getUser2() != null && conv.getUser2().getUserId().equals(userId);

            if (!isUser1 && !isUser2) {
                throw new SecurityException("You are not a participant of this conversation");
            }

            if (isUser1) {
                conv.setUser1Nickname(newName.trim());
            } else {
                conv.setUser2Nickname(newName.trim());
            }
            conversationRepo.save(conv);
            log.info("Private chat {} nicknamed to '{}' by user {}", conversationId, newName, userId);

            // Broadcast only to the user who renamed it
            ConversationDto payload = toConversationDto(conv, userId);
            messagingTemplate.convertAndSend("/topic/chat/user/" + userId + "/group-renamed", payload);
        }

        return toConversationDto(conv, userId);
    }

    @Override
    @Transactional
    public ChatMessageDto recallMessage(Long messageId, Integer userId) {
        ChatMessage message = messageRepo.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!message.getSender().getUserId().equals(userId)) {
            throw new SecurityException("You can only recall your own messages");
        }

        // Replace content with recall placeholder
        message.setContent("Tin nhắn đã được thu hồi");
        message.setMessageType(MessageType.SYSTEM);
        messageRepo.save(message);

        ChatMessageDto dto = toMessageDto(message);
        ChatConversation conv = message.getConversation();

        // Broadcast the recall to all participants
        if (Boolean.TRUE.equals(conv.getIsGroup())) {
            for (User p : conv.getParticipants()) {
                messagingTemplate.convertAndSend("/topic/chat/user/" + p.getUserId() + "/message-recalled", dto);
            }
        } else {
            if (conv.getUser1() != null) {
                messagingTemplate
                        .convertAndSend("/topic/chat/user/" + conv.getUser1().getUserId() + "/message-recalled", dto);
            }
            if (conv.getUser2() != null) {
                messagingTemplate
                        .convertAndSend("/topic/chat/user/" + conv.getUser2().getUserId() + "/message-recalled", dto);
            }
        }

        log.info("Message {} recalled by user {}", messageId, userId);
        return dto;
    }

    @Override
    @Transactional
    public void leaveGroupChat(Long conversationId, Integer userId) {
        ChatConversation conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

        if (!Boolean.TRUE.equals(conv.getIsGroup())) {
            throw new IllegalArgumentException("Cannot leave a 1-on-1 conversation");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!conv.getParticipants().contains(user)) {
            throw new SecurityException("You are not a participant of this group");
        }

        conv.getParticipants().remove(user);
        conversationRepo.save(conv);

        // Send system message so others know
        String systemMsg = user.getFullName() + " đã rời khỏi nhóm.";
        ChatMessage message = ChatMessage.builder()
                .conversation(conv)
                .sender(user)
                .content(systemMsg)
                .messageType(MessageType.SYSTEM)
                .sentAt(LocalDateTime.now())
                .build();
        message = messageRepo.save(message);

        // Update conversation preview
        conv.setLastMessagePreview(systemMsg);
        conv.setLastMessageAt(message.getSentAt());
        conversationRepo.save(conv);

        ChatMessageDto messageDto = toMessageDto(message);
        for (User p : conv.getParticipants()) {
            messagingTemplate.convertAndSend("/topic/chat/user/" + p.getUserId(), messageDto);
        }
        log.info("User {} left group chat {}", userId, conversationId);
    }

    // ── Private helpers ──────────────────────────────────────────

    private String cleanPreview(ChatConversation conv, Integer currentUserId) {
        String preview = conv.getLastMessagePreview();
        if (preview != null && preview.contains("\"action\"") && preview.contains("{")) {
            Page<ChatMessage> msgs = messageRepo.findByConversationId(conv.getConversationId(), currentUserId,
                    PageRequest.of(0, 20));
            return msgs.getContent().stream()
                    .filter(m -> m.getMessageType() != MessageType.SYSTEM || !m.getContent().contains("\"action\""))
                    .findFirst()
                    .map(m -> {
                        String p = m.getContent();
                        return p.length() > 100 ? p.substring(0, 100) + "..." : p;
                    })
                    .orElse("");
        }
        return preview;
    }

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
                    .lastMessagePreview(cleanPreview(conv, currentUserId))
                    .lastMessageAt(conv.getLastMessageAt())
                    .unreadCount(unread)
                    .blocked(false)
                    .blockedByThem(false)
                    .build();
        } else {
            User otherUser = (conv.getUser1() != null && conv.getUser1().getUserId().equals(currentUserId))
                    ? conv.getUser2()
                    : conv.getUser1();

            boolean isUser1 = conv.getUser1() != null && conv.getUser1().getUserId().equals(currentUserId);
            String displayOtherUserName = otherUser.getFullName();

            if (isUser1 && conv.getUser1Nickname() != null && !conv.getUser1Nickname().trim().isEmpty()) {
                displayOtherUserName = conv.getUser1Nickname();
            } else if (!isUser1 && conv.getUser2Nickname() != null && !conv.getUser2Nickname().trim().isEmpty()) {
                displayOtherUserName = conv.getUser2Nickname();
            }

            // Determine block status
            boolean isBlocked = false;
            boolean isBlockedByThem = false;
            Integer blockedByUserId = null;

            if (Boolean.TRUE.equals(conv.getIsMutualBlock())) {
                isBlocked = true;
                isBlockedByThem = true;
            } else if (conv.getBlockedBy() != null) {
                blockedByUserId = conv.getBlockedBy().getUserId();
                if (blockedByUserId.equals(currentUserId)) {
                    isBlocked = true; // I blocked them
                } else {
                    isBlockedByThem = true; // They blocked me
                }
            }

            return ConversationDto.builder()
                    .conversationId(conv.getConversationId())
                    .isGroup(false)
                    .otherUserId(otherUser.getUserId())
                    .otherUserName(displayOtherUserName)
                    .otherUserAvatar(otherUser.getAvatarUrl())
                    .otherUserOnline(false) // Will be updated by WebSocket presence
                    .lastMessagePreview(
                            isBlockedByThem ? "Bạn đã bị chặn bởi người này" : cleanPreview(conv, currentUserId))
                    .lastMessageAt(conv.getLastMessageAt())
                    .unreadCount(unread)
                    .blocked(isBlocked)
                    .blockedByThem(isBlockedByThem)
                    .blockedByUserId(blockedByUserId)
                    .build();
        }
    }

    private ChatMessageDto toMessageDto(ChatMessage msg) {
        User sender = msg.getSender();

        java.util.List<String> readAvatars = new java.util.ArrayList<>();
        java.util.List<String> readNames = new java.util.ArrayList<>();
        if (msg.getReadBy() != null) {
            msg.getReadBy().stream()
                    .filter(u -> !u.getUserId().equals(sender.getUserId()))
                    .forEach(u -> {
                        readAvatars.add(u.getAvatarUrl());
                        readNames.add(u.getFullName() != null ? u.getFullName() : "User");
                    });
        }

        return ChatMessageDto.builder()
                .messageId(msg.getMessageId())
                .conversationId(msg.getConversation().getConversationId())
                .senderId(sender.getUserId())
                .senderName(sender.getFullName())
                .senderAvatar(sender.getAvatarUrl())
                .content(msg.getContent())
                .messageType(msg.getMessageType().name())
                .isRead(msg.getIsRead() || !msg.getReadBy().isEmpty())
                .readByAvatars(readAvatars)
                .readByNames(readNames)
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
            return "👋 Xin chào! Tôi là trợ lý AI của SportsBook.\n\n"
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
                    + "• Email: support@sportsbook.vn\n"
                    + "• Chat trực tiếp với đội ngũ hỗ trợ qua hệ thống này\n\n"
                    + "Hoặc bạn có thể mô tả vấn đề, tôi sẽ cố gắng giúp!";
        }

        return defaultSmartReply();
    }

    private String defaultSmartReply() {
        return "🤖 Cảm ơn bạn đã liên hệ! Tôi là trợ lý AI của SportsBook.\n\n"
                + "Tôi có thể giúp bạn với:\n"
                + "• 🏟️ Đặt sân thể thao\n"
                + "• 💰 Thông tin giá cả\n"
                + "• ❌ Hủy / hoàn tiền\n"
                + "• 💳 Thanh toán\n"
                + "• ⚽ Ghép kèo\n"
                + "• ⭐ Đánh giá sân\n\n"
                + "Hãy hỏi tôi bất cứ điều gì bạn cần!";
    }

    @Override
    @Transactional
    public void hideMessage(Long messageId, Integer userId) {
        ChatMessage message = messageRepo.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Add the user to the hidden list
        message.getHiddenBy().add(user);
        messageRepo.save(message);

        // Broadcast to this user only so their UI instantly drops the message
        String payload = "{\"type\":\"message_hidden\",\"messageId\":" + messageId + "}";
        messagingTemplate.convertAndSend("/topic/chat/user/" + userId + "/message-status", payload);
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId, Integer userId) {
        ChatConversation conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Verify user is part of the conversation
        if (Boolean.TRUE.equals(conversation.getIsGroup())) {
            boolean isMember = conversation.getParticipants().stream()
                    .anyMatch(u -> u.getUserId().equals(userId));
            if (!isMember) {
                throw new IllegalArgumentException("You are not part of this group conversation");
            }
        } else {
            if (!conversation.getUser1().getUserId().equals(userId) &&
                    !conversation.getUser2().getUserId().equals(userId)) {
                throw new IllegalArgumentException("You are not part of this conversation");
            }
        }

        // Hide conversation for this user by upserting the state
        ChatConversationHiddenState state = hiddenStateRepo
                .findByConversationIdAndUserId(conversationId, userId)
                .orElse(new ChatConversationHiddenState(conversationId, userId, conversation,
                        userRepo.findById(userId).orElseThrow(), null));

        state.setHiddenAt(LocalDateTime.now());
        hiddenStateRepo.save(state);

        // Broadcast to this user only so their UI instantly drops the conversation
        String payload = "{\"type\":\"conversation_hidden\",\"conversationId\":" + conversationId + "}";
        messagingTemplate.convertAndSend("/topic/chat/user/" + userId + "/conversation-status", payload);
    }

    @Override
    @Transactional
    public BlockStatusDto blockUser(Integer blockedUserId, Integer currentUserId) {
        validateBlockUsers(blockedUserId, currentUserId);
        User currentUser = getUser(currentUserId);
        User blockedUser = getUser(blockedUserId);
        ChatConversation conv = getOrCreateDirectConversation(currentUser, blockedUser);

        if (conv.getBlockedBy() == null) {
            conv.setBlockedBy(currentUser);
            conv.setBlockedAt(LocalDateTime.now());
        } else if (!conv.getBlockedBy().getUserId().equals(currentUserId)
                && !Boolean.TRUE.equals(conv.getIsMutualBlock())) {
            conv.setIsMutualBlock(true);
            conv.setBlockedAt(LocalDateTime.now());
        }

        conversationRepo.save(conv);
        log.info("User {} blocked user {}", currentUserId, blockedUserId);
        return publishBlockState(conv, currentUserId, blockedUserId);
    }

    @Override
    @Transactional
    public BlockStatusDto unblockUser(Integer blockedUserId, Integer currentUserId) {
        validateBlockUsers(blockedUserId, currentUserId);
        ChatConversation conv = conversationRepo.findByUserPair(
                        Math.min(currentUserId, blockedUserId), Math.max(currentUserId, blockedUserId))
                .orElse(null);

        if (conv == null) {
            return BlockStatusDto.builder().userId(blockedUserId).build();
        }

        if (Boolean.TRUE.equals(conv.getIsMutualBlock())) {
            conv.setIsMutualBlock(false);
            if (conv.getBlockedBy().getUserId().equals(currentUserId)) {
                conv.setBlockedBy(getUser(blockedUserId));
            }
            conversationRepo.save(conv);
        } else if (conv.getBlockedBy() != null && conv.getBlockedBy().getUserId().equals(currentUserId)) {
            conv.setBlockedBy(null);
            conv.setBlockedAt(null);
            conversationRepo.save(conv);
        }

        log.info("User {} unblocked user {}", currentUserId, blockedUserId);
        return publishBlockState(conv, currentUserId, blockedUserId);
    }

    private void validateBlockUsers(Integer otherUserId, Integer currentUserId) {
        if (otherUserId == null || currentUserId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (otherUserId.equals(currentUserId)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }
    }

    private User getUser(Integer userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private ChatConversation getOrCreateDirectConversation(User currentUser, User otherUser) {
        Integer currentUserId = currentUser.getUserId();
        Integer otherUserId = otherUser.getUserId();
        Integer u1 = Math.min(currentUserId, otherUserId);
        Integer u2 = Math.max(currentUserId, otherUserId);
        return conversationRepo.findByUserPair(u1, u2).orElseGet(() -> conversationRepo.save(
                ChatConversation.builder()
                        .user1(u1.equals(currentUserId) ? currentUser : otherUser)
                        .user2(u2.equals(currentUserId) ? currentUser : otherUser)
                        .isGroup(false)
                        .build()));
    }

    private BlockStatusDto publishBlockState(ChatConversation conv, Integer currentUserId, Integer otherUserId) {
        BlockStatusDto currentState = blockStateFor(conv, currentUserId, otherUserId);
        BlockStatusDto otherState = blockStateFor(conv, otherUserId, currentUserId);
        messagingTemplate.convertAndSend("/topic/chat/user/" + currentUserId + "/block-status", currentState);
        messagingTemplate.convertAndSend("/topic/chat/user/" + otherUserId + "/block-status", otherState);
        return currentState;
    }

    private BlockStatusDto blockStateFor(ChatConversation conv, Integer viewerId, Integer otherUserId) {
        boolean mutual = Boolean.TRUE.equals(conv.getIsMutualBlock());
        Integer blockerId = conv.getBlockedBy() == null ? null : conv.getBlockedBy().getUserId();
        boolean blocked = mutual || viewerId.equals(blockerId);
        boolean blockedByThem = mutual || otherUserId.equals(blockerId);
        return BlockStatusDto.builder()
                .userId(otherUserId)
                .blocked(blocked)
                .blockedByThem(blockedByThem)
                .mutual(mutual)
                .build();
    }
}
