package com.sportvenue.security;

import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.repository.ComplaintRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private WebSocketAuthChannelInterceptor interceptor;

    @Test
    void preSend_ConnectWithBlockedUser_AllowsConnection() {
        String token = "valid-token";
        String email = "blocked@example.com";
        User blockedUser = User.builder()
                .userId(10)
                .email(email)
                .firstName("Blocked")
                .lastName("User")
                .role(Role.builder().roleName("Customer").build())
                .accountStatus(AccountStatus.BLOCKED)
                .build();

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromJWT(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(new UserPrincipal(blockedUser));

        Message<?> connectMsg = connectMessage(token);
        Message<?> result = assertDoesNotThrow(() -> interceptor.preSend(connectMsg, null));
        assertNotNull(result);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertNotNull(accessor.getUser());
    }

    @Test
    void preSend_SubscribeToOwnStatusTopicWhenBlocked_AllowsSubscription() {
        User blockedUser = User.builder()
                .userId(10)
                .role(Role.builder().roleName("Customer").build())
                .accountStatus(AccountStatus.BLOCKED)
                .build();

        Message<?> subscribeMsg = subscribeMessage(blockedUser, "/topic/user/10/account-status");
        assertDoesNotThrow(() -> interceptor.preSend(subscribeMsg, null));
    }

    @Test
    void preSend_SubscribeToChatTopicWhenBlocked_RejectsSubscription() {
        User blockedUser = User.builder()
                .userId(10)
                .role(Role.builder().roleName("Customer").build())
                .accountStatus(AccountStatus.BLOCKED)
                .build();

        Message<?> subscribeMsg = subscribeMessage(blockedUser, "/topic/chat/user/10");
        assertThrows(MessagingException.class, () -> interceptor.preSend(subscribeMsg, null));
    }

    @Test
    void preSend_SubscribeToOthersChatTopicWhenActive_RejectsSubscription() {
        User activeUser = User.builder()
                .userId(10)
                .role(Role.builder().roleName("Customer").build())
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Message<?> subscribeMsg = subscribeMessage(activeUser, "/topic/chat/user/20");
        assertThrows(MessagingException.class, () -> interceptor.preSend(subscribeMsg, null));
    }

    @Test
    void preSend_SubscribeToOwnChatTopicWhenActive_AllowsSubscription() {
        User activeUser = User.builder()
                .userId(10)
                .role(Role.builder().roleName("Customer").build())
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        Message<?> subscribeMsg = subscribeMessage(activeUser, "/topic/chat/user/10/new-group");
        assertDoesNotThrow(() -> interceptor.preSend(subscribeMsg, null));
    }

    private Message<byte[]> connectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeMessage(User user, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        accessor.setUser(auth);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
