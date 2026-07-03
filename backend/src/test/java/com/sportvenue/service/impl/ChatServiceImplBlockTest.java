package com.sportvenue.service.impl;

import com.sportvenue.dto.chat.BlockStatusDto;
import com.sportvenue.entity.ChatConversation;
import com.sportvenue.entity.User;
import com.sportvenue.repository.ChatConversationHiddenStateRepository;
import com.sportvenue.repository.ChatConversationRepository;
import com.sportvenue.repository.ChatMessageRepository;
import com.sportvenue.repository.ChatbotLogRepository;
import com.sportvenue.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplBlockTest {

    @Mock
    private ChatConversationRepository conversationRepo;
    @Mock
    private ChatMessageRepository messageRepo;
    @Mock
    private ChatbotLogRepository chatbotLogRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private ChatConversationHiddenStateRepository hiddenStateRepo;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ChatServiceImpl service;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(conversationRepo, messageRepo, chatbotLogRepo, userRepo,
                hiddenStateRepo, messagingTemplate);
        user1 = User.builder().userId(1).build();
        user2 = User.builder().userId(2).build();
    }

    @Test
    void blockUserCreatesConversationWhenUsersHaveNeverChatted() {
        when(userRepo.findById(1)).thenReturn(Optional.of(user1));
        when(userRepo.findById(2)).thenReturn(Optional.of(user2));
        when(conversationRepo.findByUserPair(1, 2)).thenReturn(Optional.empty());
        when(conversationRepo.save(any(ChatConversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BlockStatusDto result = service.blockUser(2, 1);

        assertTrue(result.isBlocked());
        assertFalse(result.isBlockedByThem());
        assertEquals(2, result.getUserId());
        verify(conversationRepo, atLeastOnce()).save(any(ChatConversation.class));
        verify(messagingTemplate).convertAndSend("/topic/chat/user/1/block-status", result);
    }

    @Test
    void blockUserRejectsSelfBlock() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.blockUser(1, 1));

        assertEquals("Cannot block yourself", error.getMessage());
        verify(conversationRepo, never()).findByUserPair(any(), any());
    }

    @Test
    void unblockUserDoesNotToggleOtherUsersBlock() {
        ChatConversation conversation = directConversation(user2, false);
        when(conversationRepo.findByUserPair(1, 2)).thenReturn(Optional.of(conversation));

        BlockStatusDto result = service.unblockUser(2, 1);

        assertFalse(result.isBlocked());
        assertTrue(result.isBlockedByThem());
        verify(conversationRepo, never()).save(any());
    }

    @Test
    void originalBlockerCanLeaveMutualBlockWithoutRemovingOtherUsersBlock() {
        ChatConversation conversation = directConversation(user1, true);
        when(conversationRepo.findByUserPair(1, 2)).thenReturn(Optional.of(conversation));
        when(userRepo.findById(2)).thenReturn(Optional.of(user2));

        BlockStatusDto result = service.unblockUser(2, 1);

        assertFalse(result.isBlocked());
        assertTrue(result.isBlockedByThem());
        assertFalse(result.isMutual());
        assertEquals(user2, conversation.getBlockedBy());
    }

    private ChatConversation directConversation(User blockedBy, boolean mutual) {
        return ChatConversation.builder()
                .user1(user1)
                .user2(user2)
                .blockedBy(blockedBy)
                .isMutualBlock(mutual)
                .build();
    }
}
