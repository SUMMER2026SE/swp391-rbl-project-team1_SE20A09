package com.sportvenue.service.ai;

import java.util.List;
import java.util.Map;

public interface AgentToolProvider {
    List<Map<String, Object>> getToolDefinitions();

    Object executeTool(String toolName, String jsonArguments, Integer userId);
}
