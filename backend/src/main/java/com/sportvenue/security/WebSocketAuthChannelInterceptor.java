package com.sportvenue.security;

import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.repository.ComplaintRepository;
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
    private final ComplaintRepository complaintRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
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
            
            // Allow BLOCKED users to connect to WebSockets so they can receive their unlock event.
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
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new MessagingException("WebSocket SUBSCRIBE rejected: missing destination");
        }

        Object principalObj = accessor.getUser();
        if (!(principalObj instanceof UsernamePasswordAuthenticationToken authToken)) {
            throw new MessagingException("WebSocket SUBSCRIBE rejected: unauthenticated");
        }

        UserDetails userDetails = (UserDetails) authToken.getPrincipal();
        if (!(userDetails instanceof UserPrincipal userPrincipal)) {
            throw new MessagingException("WebSocket SUBSCRIBE rejected: invalid principal");
        }

        User user = userPrincipal.getUser();
        boolean isBlocked = user.getAccountStatus() == AccountStatus.BLOCKED;

        validateSubscription(user, destination, isBlocked);
    }

    private void validateSubscription(User user, String destination, boolean isBlocked) {
        Integer userId = user.getUserId();

        // 1. Account status topic: /topic/user/{userId}/account-status
        String accountStatusPrefix = "/topic/user/";
        if (destination.startsWith(accountStatusPrefix) && destination.endsWith("/account-status")) {
            String userIdStr = destination.substring(accountStatusPrefix.length(), destination.length() - "/account-status".length());
            try {
                Integer targetUserId = Integer.parseInt(userIdStr);
                if (targetUserId.equals(userId)) {
                    // Allowed for everyone, including BLOCKED users
                    return;
                }
            } catch (NumberFormatException e) {
                // fall through to reject
            }
            throw new MessagingException("WebSocket SUBSCRIBE rejected: unauthorized topic access");
        }

        // If user is BLOCKED, they are ONLY allowed to subscribe to their own account-status topic.
        if (isBlocked) {
            throw new MessagingException("WebSocket SUBSCRIBE rejected: account is blocked");
        }

        // 2. Chat topics: /topic/chat/user/{userId}(/...) or /topic/chat/typing/{userId}
        String chatUserPrefix = "/topic/chat/user/";
        if (destination.startsWith(chatUserPrefix)) {
            String remaining = destination.substring(chatUserPrefix.length());
            int slashIndex = remaining.indexOf('/');
            String userIdStr = slashIndex == -1 ? remaining : remaining.substring(0, slashIndex);
            try {
                Integer targetUserId = Integer.parseInt(userIdStr);
                if (targetUserId.equals(userId)) {
                    return;
                }
            } catch (NumberFormatException e) {
                // fall through
            }
            throw new MessagingException("WebSocket SUBSCRIBE rejected: unauthorized chat topic access");
        }

        String chatTypingPrefix = "/topic/chat/typing/";
        if (destination.startsWith(chatTypingPrefix)) {
            String userIdStr = destination.substring(chatTypingPrefix.length());
            try {
                Integer targetUserId = Integer.parseInt(userIdStr);
                if (targetUserId.equals(userId)) {
                    return;
                }
            } catch (NumberFormatException e) {
                // fall through
            }
            throw new MessagingException("WebSocket SUBSCRIBE rejected: unauthorized typing topic access");
        }

        // 3. Complaint topic: /topic/complaint/{complaintId}
        String complaintPrefix = "/topic/complaint/";
        if (destination.startsWith(complaintPrefix)) {
            String complaintIdStr = destination.substring(complaintPrefix.length());
            try {
                Integer complaintId = Integer.parseInt(complaintIdStr);
                if (canAccessComplaint(user, complaintId)) {
                    return;
                }
            } catch (NumberFormatException e) {
                // fall through
            }
            throw new MessagingException("WebSocket SUBSCRIBE rejected: unauthorized complaint topic access");
        }

        // Reject all other topics to enforce strict security
        throw new MessagingException("WebSocket SUBSCRIBE rejected: invalid destination topic");
    }

    private boolean canAccessComplaint(User user, Integer complaintId) {
        if ("Admin".equalsIgnoreCase(user.getRole().getRoleName())) {
            return true;
        }

        return complaintRepository.findById(complaintId)
                .map(complaint -> {
                    if (complaint.getUser() != null && complaint.getUser().getUserId().equals(user.getUserId())) {
                        return true;
                    }
                    if (complaint.getBooking() != null
                            && complaint.getBooking().getStadium() != null
                            && complaint.getBooking().getStadium().getOwner() != null
                            && complaint.getBooking().getStadium().getOwner().getUser() != null
                            && complaint.getBooking().getStadium().getOwner().getUser().getUserId().equals(user.getUserId())) {
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }
}
