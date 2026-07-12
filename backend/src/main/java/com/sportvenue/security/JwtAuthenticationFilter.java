package com.sportvenue.security;

import com.sportvenue.entity.enums.AccountStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String LOCKED_ACCOUNT_MESSAGE =
            "{\"message\": \"Tài khoản của bạn đang bị khóa. Vui lòng gửi kháng cáo.\"}";

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   CustomUserDetailsService customUserDetailsService) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String email = tokenProvider.getEmailFromJWT(jwt);

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                if (!userDetails.isAccountNonLocked() || !userDetails.isEnabled()) {
                    log.warn("Blocked user {} attempted to authenticate", email);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(LOCKED_ACCOUNT_MESSAGE);
                    return;
                }

                if (isBlockedUser(userDetails) && !isBlockedUserAllowedPath(request)) {
                    log.warn("Blocked user {} attempted restricted path {}", email, request.getRequestURI());
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(LOCKED_ACCOUNT_MESSAGE);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isBlockedUser(UserDetails userDetails) {
        return userDetails instanceof UserPrincipal userPrincipal
                && userPrincipal.getUser().getAccountStatus() == AccountStatus.BLOCKED;
    }

    private boolean isBlockedUserAllowedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/v1/auth/me")
                || path.equals("/api/v1/appeals")
                || path.startsWith("/api/v1/appeals/");
    }
}
