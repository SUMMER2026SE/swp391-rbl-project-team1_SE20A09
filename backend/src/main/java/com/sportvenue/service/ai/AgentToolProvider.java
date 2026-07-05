package com.sportvenue.service.ai;

import java.util.List;
import java.util.Map;

/**
 * Bộ tool + system prompt riêng cho 1 role (Customer/Owner/Admin). Thêm role mới chỉ cần thêm
 * 1 bean implements interface này — {@link AgentRegistry} tự nhận diện qua {@link #getRoleName()},
 * không cần sửa AiChatServiceImpl.
 */
public interface AgentToolProvider {
    /** Tên role agent này phục vụ — phải khớp User.role.roleName ("Customer", "Owner", "Admin"). */
    String getRoleName();

    /** System prompt cho agent này. {@code userId == null} nghĩa là khách vãng lai (guest). */
    String getSystemPrompt(Integer userId);

    List<Map<String, Object>> getToolDefinitions();

    Object executeTool(String toolName, String jsonArguments, Integer userId);
}
