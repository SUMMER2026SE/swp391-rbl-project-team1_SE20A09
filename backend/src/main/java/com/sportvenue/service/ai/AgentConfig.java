package com.sportvenue.service.ai;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AgentConfig {
    private final String systemPrompt;
    private final List<Map<String, Object>> tools;
}
