package com.sportvenue.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvenue.dto.request.AiChatRequest;
import com.sportvenue.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final CustomerAgentToolProvider customerAgentToolProvider;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Value("${app.ai.base-url:https://api.groq.com/openai/v1}")
    private String aiBaseUrl;

    @Value("${app.ai.api-key}")
    private String aiApiKey;

    @Value("${app.ai.model:llama-3.3-70b-versatile}")
    private String aiModel;

    private static final String CUSTOMER_SYSTEM_PROMPT = 
            "Bạn là trợ lý ảo AI chính thức của SportHub, một nền tảng đặt sân thể thao trực tuyến tại Việt Nam. " +
            "Nhiệm vụ của bạn là giúp khách hàng tìm kiếm sân đấu, xem lịch trống, tìm kèo ghép và trả lời các thông tin liên quan đến đặt sân. " +
            "Hãy luôn thân thiện, chuyên nghiệp và trả lời bằng tiếng Việt. " +
            "Khi gọi công cụ (tools), hãy sử dụng thông tin trả về từ công cụ để trả lời chính xác nhất. Nếu công cụ trả về danh sách sân đấu, hãy cung cấp chi tiết như tên sân, địa chỉ, giá cả cho người dùng.";

    @Override
    public void handleChatStream(AiChatRequest request, ResponseBodyEmitter emitter, UserPrincipal userPrincipal,
                                 AtomicReference<InputStream> activeStreamRef) {
        Integer userId = userPrincipal != null ? userPrincipal.getUserId() : null;
        log.info("Starting chat stream for user: {} with model: {}", userId, aiModel);

        try {
            if (aiApiKey == null || aiApiKey.isBlank()) {
                throw new IllegalStateException("GROQ_API_KEY chưa được cấu hình ở Backend.");
            }

            log.info("Received request payload: {}", objectMapper.writeValueAsString(request));

            // 1. Build the chat history for LLM
            List<Map<String, Object>> chatHistory = new ArrayList<>();
            chatHistory.add(Map.of("role", "system", "content", CUSTOMER_SYSTEM_PROMPT));

            if (request.getMessages() != null) {
                for (AiChatRequest.Message reqMsg : request.getMessages()) {
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("role", reqMsg.getRole());
                    String content = reqMsg.getContent();
                    if (content == null || content.isBlank()) {
                        if (reqMsg.getParts() != null) {
                            StringBuilder partsText = new StringBuilder();
                            for (AiChatRequest.MessagePart part : reqMsg.getParts()) {
                                if ("text".equals(part.getType()) && part.getText() != null) {
                                    partsText.append(part.getText());
                                }
                            }
                            content = partsText.toString();
                        }
                    }
                    if (content == null) {
                        content = "";
                    }
                    msgMap.put("content", content);
                    
                    if (reqMsg.getToolCalls() != null && !reqMsg.getToolCalls().isEmpty()) {
                        List<Map<String, Object>> toolCallsList = new ArrayList<>();
                        for (AiChatRequest.ToolCall tc : reqMsg.getToolCalls()) {
                            toolCallsList.add(Map.of(
                                "id", tc.getId(),
                                "type", "function",
                                "function", Map.of(
                                    "name", tc.getFunction().getName(),
                                    "arguments", tc.getFunction().getArguments()
                                )
                            ));
                        }
                        msgMap.put("tool_calls", toolCallsList);
                    }
                    if (reqMsg.getToolCallId() != null) {
                        msgMap.put("tool_call_id", reqMsg.getToolCallId());
                    }
                    if (reqMsg.getName() != null) {
                        msgMap.put("name", reqMsg.getName());
                    }
                    chatHistory.add(msgMap);
                }
            }

            log.info("Built chat history: {}", objectMapper.writeValueAsString(chatHistory));

            boolean keepRunning = true;
            int toolCallCount = 0;
            final int MAX_TOOL_CALLS = 10;

            while (keepRunning) {
                // 2. Prepare parameters
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", aiModel);
                requestBody.put("messages", chatHistory);
                requestBody.put("stream", true);
                requestBody.put("temperature", 0.0); // Set temperature to 0.0 for tool call stability
                
                List<Map<String, Object>> toolDefs = customerAgentToolProvider.getToolDefinitions();
                if (toolDefs != null && !toolDefs.isEmpty()) {
                    requestBody.put("tools", toolDefs);
                }

                String requestJson = objectMapper.writeValueAsString(requestBody);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(aiBaseUrl + "/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + aiApiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .build();

                // 3. Open raw stream connection
                HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    String errorMsg = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                    throw new RuntimeException("LLM Gateway returned error: " + errorMsg);
                }

                InputStream is = response.body();
                activeStreamRef.set(is);

                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;

                Map<Integer, ToolCallAccumulator> accumulatorMap = new TreeMap<>();
                StringBuilder assistantText = new StringBuilder();
                String textPartId = "text-" + UUID.randomUUID().toString();
                boolean textStarted = false;

                long startTime = System.currentTimeMillis();

                while ((line = reader.readLine()) != null) {
                    log.info("Raw response line from LLM: {}", line);
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }

                        JsonNode chunkNode = objectMapper.readTree(data);
                        if (chunkNode.hasNonNull("error")) {
                            String errorMsg = chunkNode.get("error").hasNonNull("message")
                                    ? chunkNode.get("error").get("message").asText()
                                    : "Unknown LLM stream error";
                            throw new RuntimeException("LLM Stream Error: " + errorMsg);
                        }

                        if (chunkNode.hasNonNull("choices") && chunkNode.get("choices").size() > 0) {
                            JsonNode choice = chunkNode.get("choices").get(0);
                            if (choice.hasNonNull("delta")) {
                                JsonNode delta = choice.get("delta");

                                // 4. Stream text delta to client
                                if (delta.hasNonNull("content")) {
                                    String contentText = delta.get("content").asText();
                                    assistantText.append(contentText);
                                    if (!textStarted) {
                                        textStarted = true;
                                        Map<String, Object> textStart = Map.of(
                                            "type", "text-start",
                                            "id", textPartId
                                        );
                                        emitter.send("event: text-start\ndata: " + objectMapper.writeValueAsString(textStart) + "\n\n");
                                    }
                                    Map<String, Object> textDelta = Map.of(
                                        "type", "text-delta",
                                        "id", textPartId,
                                        "delta", contentText
                                    );
                                    emitter.send("event: text-delta\ndata: " + objectMapper.writeValueAsString(textDelta) + "\n\n");
                                }

                                // 5. Accumulate tool call delta chunks (Sub-task 2A)
                                if (delta.hasNonNull("tool_calls")) {
                                    JsonNode toolCallsNode = delta.get("tool_calls");
                                    for (JsonNode tcNode : toolCallsNode) {
                                        int index = tcNode.get("index").asInt();
                                        ToolCallAccumulator acc = accumulatorMap.computeIfAbsent(index, idx -> new ToolCallAccumulator());

                                        if (tcNode.hasNonNull("id")) {
                                            acc.id = tcNode.get("id").asText();
                                        }
                                        if (tcNode.hasNonNull("type")) {
                                            acc.type = tcNode.get("type").asText();
                                        }
                                        if (tcNode.hasNonNull("function")) {
                                            JsonNode funcNode = tcNode.get("function");
                                            if (funcNode.hasNonNull("name")) {
                                                acc.name += funcNode.get("name").asText();
                                            }
                                            if (funcNode.hasNonNull("arguments")) {
                                                acc.arguments.append(funcNode.get("arguments").asText());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                reader.close();
                is.close();
                activeStreamRef.set(null);

                if (textStarted) {
                    textStarted = false; // Reset for next iteration if any
                    Map<String, Object> textEnd = Map.of(
                        "type", "text-end",
                        "id", textPartId
                    );
                    emitter.send("event: text-end\ndata: " + objectMapper.writeValueAsString(textEnd) + "\n\n");
                }

                long duration = System.currentTimeMillis() - startTime;
                log.info("LLM turn completed in {} ms. Tool calls requested: {}", duration, accumulatorMap.size());

                // 6. Handle turn decision (Sub-task 2B)
                if (accumulatorMap.isEmpty()) {
                    keepRunning = false;
                    Map<String, Object> finish = Map.of(
                        "type", "finish",
                        "finishReason", "stop"
                    );
                    emitter.send("event: finish\ndata: " + objectMapper.writeValueAsString(finish) + "\n\n");
                } else {
                    if (toolCallCount + accumulatorMap.size() > MAX_TOOL_CALLS) {
                        log.warn("Max tool calls exceeded for user: {}. Limit: {}, Attempted: {}", userId, MAX_TOOL_CALLS, toolCallCount + accumulatorMap.size());
                        Map<String, Object> error = Map.of(
                            "type", "error",
                            "errorText", "Max tool calls exceeded"
                        );
                        emitter.send("event: error\ndata: " + objectMapper.writeValueAsString(error) + "\n\n");
                        keepRunning = false;
                        break;
                    }
                    toolCallCount += accumulatorMap.size();

                    List<Map<String, Object>> assistantToolCalls = new ArrayList<>();
                    Set<String> failedToolCallIds = new HashSet<>();
                    
                    // Emit tool calls to UI and format assistant turn history
                    for (Map.Entry<Integer, ToolCallAccumulator> entry : accumulatorMap.entrySet()) {
                        ToolCallAccumulator acc = entry.getValue();
                        
                        Map<String, Object> argsMap = null;
                        try {
                            argsMap = objectMapper.readValue(acc.arguments.toString(), Map.class);
                        } catch (Exception e) {
                            log.error("Failed to parse tool call arguments: {}", acc.arguments, e);
                            failedToolCallIds.add(acc.id);
                        }

                        // Stream tool call event to client
                        Map<String, Object> toolCallEmit = Map.of(
                            "type", "tool-input-available",
                            "toolCallId", acc.id,
                            "toolName", acc.name,
                            "input", argsMap != null ? argsMap : new HashMap<>(),
                            "providerExecuted", true
                        );
                        emitter.send("event: tool-input-available\ndata: " + objectMapper.writeValueAsString(toolCallEmit) + "\n\n");

                        assistantToolCalls.add(Map.of(
                            "id", acc.id,
                            "type", "function",
                            "function", Map.of(
                                "name", acc.name,
                                "arguments", acc.arguments.toString()
                            )
                        ));
                    }

                    // Add assistant message containing tool calls to history
                    Map<String, Object> assistantMsg = new HashMap<>();
                    assistantMsg.put("role", "assistant");
                    if (assistantText.length() > 0) {
                        assistantMsg.put("content", assistantText.toString());
                    } else {
                        assistantMsg.put("content", null);
                    }
                    assistantMsg.put("tool_calls", assistantToolCalls);
                    chatHistory.add(assistantMsg);

                    // Execute tools and emit outcomes (Sub-task 2B & 2D)
                    for (Map.Entry<Integer, ToolCallAccumulator> entry : accumulatorMap.entrySet()) {
                        ToolCallAccumulator acc = entry.getValue();

                        Object toolResult;
                        if (failedToolCallIds.contains(acc.id)) {
                            toolResult = Map.of("error", "Invalid arguments: Failed to parse tool call arguments JSON.");
                        } else {
                            try {
                                // Execute actual database calls (via service scope check)
                                toolResult = customerAgentToolProvider.executeTool(acc.name, acc.arguments.toString(), userId);
                            } catch (Exception e) {
                                log.error("Failed to execute tool: {}", acc.name, e);
                                toolResult = Map.of("error", "Tool execution failed: " + e.getMessage());
                            }
                        }

                        String toolResultContent;
                        if (toolResult instanceof Map && ((Map<?, ?>) toolResult).containsKey("error")) {
                            toolResultContent = "Error: " + ((Map<?, ?>) toolResult).get("error").toString();
                        } else {
                            toolResultContent = objectMapper.writeValueAsString(toolResult);
                        }

                        // Stream tool result event to client
                        Map<String, Object> toolResultEmit = Map.of(
                            "type", "tool-output-available",
                            "toolCallId", acc.id,
                            "output", toolResult,
                            "providerExecuted", true
                        );
                        emitter.send("event: tool-output-available\ndata: " + objectMapper.writeValueAsString(toolResultEmit) + "\n\n");

                        // Add tool result payload to history
                        chatHistory.add(Map.of(
                            "role", "tool",
                            "tool_call_id", acc.id,
                            "name", acc.name,
                            "content", toolResultContent
                        ));
                    }
                    
                    // Loop back to send results to LLM for final generation
                }
            }

            emitter.complete();
            log.info("SSE chat stream successfully completed for user: {}", userId);

        } catch (Exception e) {
            log.error("Exception in AI chat stream processing for user: {}", userId, e);
            try {
                Map<String, Object> error = Map.of(
                    "type", "error",
                    "errorText", "Đã xảy ra lỗi: " + e.getMessage()
                );
                emitter.send("event: error\ndata: " + objectMapper.writeValueAsString(error) + "\n\n");
            } catch (Exception ex) {
                log.error("Failed to stream error to emitter", ex);
            }
            emitter.completeWithError(e);
        }
    }

    public static class ToolCallAccumulator {
        public String id;
        public String type = "function";
        public String name = "";
        public StringBuilder arguments = new StringBuilder();
    }
}
