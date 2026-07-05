package com.sportvenue.service.ai;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chọn đúng {@link AgentToolProvider} theo role của user hiện tại (Ticket 2C). Spring tự inject
 * tất cả bean implements AgentToolProvider vào đây — thêm OwnerAgentToolProvider/
 * AdminAgentToolProvider sau này không cần sửa AiChatServiceImpl hay class này.
 */
@Component
public class AgentRegistry {

    private final Map<String, AgentToolProvider> providersByRole;
    private final AgentToolProvider defaultProvider;

    public AgentRegistry(List<AgentToolProvider> providers) {
        this.providersByRole = providers.stream()
                .collect(Collectors.toMap(AgentToolProvider::getRoleName, p -> p));
        // Customer là agent mặc định cho guest (chưa đăng nhập) — 3 tool hiện có đều là data public.
        this.defaultProvider = providersByRole.get("Customer");
    }

    /** @param roleName role của user hiện tại, hoặc null nếu là khách vãng lai (guest). */
    public AgentToolProvider resolve(String roleName) {
        if (roleName == null) {
            return defaultProvider;
        }
        return providersByRole.getOrDefault(roleName, defaultProvider);
    }
}
