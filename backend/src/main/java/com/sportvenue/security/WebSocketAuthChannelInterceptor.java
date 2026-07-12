package com.sportvenue.security;

import com.sportvenue.entity.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new MessagingException("WebSocket CONNECT rejected: missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtTokenProvider.validateToken(token)) {
                throw new MessagingException("WebSocket CONNECT rejected: invalid JWT token");
            }
            String email = jwtTokenProvider.getEmailFromJWT(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (isBlockedUser(userDetails)) {
                log.warn("Blocked user {} attempted WebSocket CONNECT", email);
                throw new MessagingException("WebSocket CONNECT rejected: account is blocked");
            }
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            accessor.setUser(auth);
            log.info("WebSocket authenticated: {}", email);
        } catch (MessagingException e) {
            throw e;
        } catch (Exception e) {
            log.warn("WebSocket JWT validation failed: {}", e.getMessage());
            throw new MessagingException("WebSocket CONNECT rejected: " + e.getMessage());
        }

        return message;
    }

    private boolean isBlockedUser(UserDetails userDetails) {
        return userDetails instanceof UserPrincipal userPrincipal
                && userPrincipal.getUser().getAccountStatus() == AccountStatus.BLOCKED;
    }
}
