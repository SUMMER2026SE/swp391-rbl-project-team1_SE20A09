package com.sportvenue.security;

import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
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
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private WebSocketAuthChannelInterceptor interceptor;

    @Test
    void preSend_ConnectWithBlockedUser_RejectsConnection() {
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

        assertThrows(MessagingException.class, () -> interceptor.preSend(connectMessage(token), null));
    }

    private Message<byte[]> connectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
