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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final CustomerAgentToolProvider customerAgentToolProvider;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static final int MAX_TOOL_CALLS = 10;

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
            "Khi gọi công cụ (tools), hãy sử dụng thông tin trả về từ công cụ để trả lời chính xác nhất. Nếu công cụ trả về danh sách sân đấu, hãy cung cấp chi tiết như tên sân, địa chỉ, giá cả cho người dùng. " +
            "Công cụ getStadiumSlots trả về TẤT CẢ khung giờ trong ngày, không lọc theo khung giờ người dùng hỏi — bạn PHẢI tự lọc lại danh sách trả về, chỉ liệt kê các khung giờ thực sự nằm trong khoảng người dùng yêu cầu (đã quy đổi đúng sang hệ 24 giờ) trước khi trả lời, không liệt kê nhầm khung giờ khác.";

    private static final String GUEST_SYSTEM_PROMPT_SUFFIX =
            " Người dùng hiện tại CHƯA đăng nhập (khách vãng lai). Bạn vẫn có thể giúp họ tìm sân, xem lịch trống và tìm kèo ghép bình thường. " +
            "Nhưng nếu họ hỏi về thông tin cá nhân, lịch sử đặt sân, hoặc muốn thực hiện đặt sân/thanh toán, hãy nhắc họ đăng nhập hoặc đăng ký tài khoản trước.";

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

            List<Map<String, Object>> chatHistory = buildChatHistory(request, userId);
            log.info("Built chat history: {}", objectMapper.writeValueAsString(chatHistory));

            boolean keepRunning = true;
            int toolCallCount = 0;

            while (keepRunning) {
                HttpResponse<InputStream> response = sendLlmTurnRequest(chatHistory);
                InputStream is = response.body();
                activeStreamRef.set(is);

                long startTime = System.currentTimeMillis();
                StreamTurnResult turn = consumeLlmStream(is, emitter);
                activeStreamRef.set(null);

                long duration = System.currentTimeMillis() - startTime;
                log.info("LLM turn completed in {} ms. Tool calls requested: {}", duration, turn.toolCalls().size());

                if (turn.toolCalls().isEmpty()) {
                    keepRunning = false;
                    emitFinish(emitter);
                } else {
                    if (toolCallCount + turn.toolCalls().size() > MAX_TOOL_CALLS) {
                        log.warn("Max tool calls exceeded for user: {}. Limit: {}, Attempted: {}",
                                userId, MAX_TOOL_CALLS, toolCallCount + turn.toolCalls().size());
                        emitMaxToolCallsError(emitter);
                        keepRunning = false;
                        break;
                    }
                    toolCallCount += turn.toolCalls().size();
                    handleToolCallsTurn(turn, chatHistory, emitter, userId);
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

    private List<Map<String, Object>> buildChatHistory(AiChatRequest request, Integer userId) {
        String systemPrompt = userId != null
                ? CUSTOMER_SYSTEM_PROMPT
                : CUSTOMER_SYSTEM_PROMPT + GUEST_SYSTEM_PROMPT_SUFFIX;
        List<Map<String, Object>> chatHistory = new ArrayList<>();
        chatHistory.add(Map.of("role", "system", "content", systemPrompt));

        if (request.getMessages() != null) {
            for (AiChatRequest.Message reqMsg : request.getMessages()) {
                chatHistory.add(convertMessageToMap(reqMsg));
            }
        }
        return chatHistory;
    }

    private Map<String, Object> convertMessageToMap(AiChatRequest.Message reqMsg) {
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
        return msgMap;
    }

    private HttpResponse<InputStream> sendLlmTurnRequest(List<Map<String, Object>> chatHistory) throws Exception {
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

        HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String errorMsg = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("LLM Gateway returned error: " + errorMsg);
        }
        return response;
    }

    private StreamTurnResult consumeLlmStream(InputStream is, ResponseBodyEmitter emitter) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;

        Map<Integer, ToolCallAccumulator> accumulatorMap = new TreeMap<>();
        StringBuilder assistantText = new StringBuilder();
        String textPartId = "text-" + UUID.randomUUID().toString();
        boolean textStarted = false;

        while ((line = reader.readLine()) != null) {
            log.info("Raw response line from LLM: {}", line);
            if (!line.startsWith("data: ")) {
                continue;
            }

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

            if (!chunkNode.hasNonNull("choices") || chunkNode.get("choices").isEmpty()) {
                continue;
            }
            JsonNode choice = chunkNode.get("choices").get(0);
            if (!choice.hasNonNull("delta")) {
                continue;
            }
            JsonNode delta = choice.get("delta");

            if (delta.hasNonNull("content")) {
                String contentText = delta.get("content").asText();
                assistantText.append(contentText);
                if (!textStarted) {
                    textStarted = true;
                    emitTextStart(emitter, textPartId);
                }
                emitTextDelta(emitter, textPartId, contentText);
            }

            if (delta.hasNonNull("tool_calls")) {
                accumulateToolCallDeltas(delta.get("tool_calls"), accumulatorMap);
            }
        }

        reader.close();
        is.close();

        if (textStarted) {
            emitTextEnd(emitter, textPartId);
        }

        return new StreamTurnResult(accumulatorMap, assistantText.toString());
    }

    private void accumulateToolCallDeltas(JsonNode toolCallsNode, Map<Integer, ToolCallAccumulator> accumulatorMap) {
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

    private void handleToolCallsTurn(StreamTurnResult turn, List<Map<String, Object>> chatHistory,
                                     ResponseBodyEmitter emitter, Integer userId) throws Exception {
        Map<Integer, ToolCallAccumulator> accumulatorMap = turn.toolCalls();
        List<Map<String, Object>> assistantToolCalls = new ArrayList<>();
        Set<String> failedToolCallIds = new HashSet<>();

        for (Map.Entry<Integer, ToolCallAccumulator> entry : accumulatorMap.entrySet()) {
            ToolCallAccumulator acc = entry.getValue();

            Map<String, Object> argsMap = null;
            try {
                argsMap = objectMapper.readValue(acc.arguments.toString(), Map.class);
            } catch (Exception e) {
                log.error("Failed to parse tool call arguments: {}", acc.arguments, e);
                failedToolCallIds.add(acc.id);
            }

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

        Map<String, Object> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", turn.assistantText().isEmpty() ? null : turn.assistantText());
        assistantMsg.put("tool_calls", assistantToolCalls);
        chatHistory.add(assistantMsg);

        executeToolsAndEmitResults(accumulatorMap, failedToolCallIds, chatHistory, emitter, userId);
    }

    private void executeToolsAndEmitResults(Map<Integer, ToolCallAccumulator> accumulatorMap, Set<String> failedToolCallIds,
                                             List<Map<String, Object>> chatHistory, ResponseBodyEmitter emitter,
                                             Integer userId) throws Exception {
        for (Map.Entry<Integer, ToolCallAccumulator> entry : accumulatorMap.entrySet()) {
            ToolCallAccumulator acc = entry.getValue();

            Object toolResult;
            if (failedToolCallIds.contains(acc.id)) {
                toolResult = Map.of("error", "Invalid arguments: Failed to parse tool call arguments JSON.");
            } else {
                try {
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

            Map<String, Object> toolResultEmit = Map.of(
                "type", "tool-output-available",
                "toolCallId", acc.id,
                "output", toolResult,
                "providerExecuted", true
            );
            emitter.send("event: tool-output-available\ndata: " + objectMapper.writeValueAsString(toolResultEmit) + "\n\n");

            chatHistory.add(Map.of(
                "role", "tool",
                "tool_call_id", acc.id,
                "name", acc.name,
                "content", toolResultContent
            ));
        }
    }

    private void emitTextStart(ResponseBodyEmitter emitter, String textPartId) throws Exception {
        Map<String, Object> textStart = Map.of("type", "text-start", "id", textPartId);
        emitter.send("event: text-start\ndata: " + objectMapper.writeValueAsString(textStart) + "\n\n");
    }

    private void emitTextDelta(ResponseBodyEmitter emitter, String textPartId, String contentText) throws Exception {
        Map<String, Object> textDelta = Map.of("type", "text-delta", "id", textPartId, "delta", contentText);
        emitter.send("event: text-delta\ndata: " + objectMapper.writeValueAsString(textDelta) + "\n\n");
    }

    private void emitTextEnd(ResponseBodyEmitter emitter, String textPartId) throws Exception {
        Map<String, Object> textEnd = Map.of("type", "text-end", "id", textPartId);
        emitter.send("event: text-end\ndata: " + objectMapper.writeValueAsString(textEnd) + "\n\n");
    }

    private void emitFinish(ResponseBodyEmitter emitter) throws Exception {
        Map<String, Object> finish = Map.of("type", "finish", "finishReason", "stop");
        emitter.send("event: finish\ndata: " + objectMapper.writeValueAsString(finish) + "\n\n");
    }

    private void emitMaxToolCallsError(ResponseBodyEmitter emitter) throws Exception {
        Map<String, Object> error = Map.of("type", "error", "errorText", "Max tool calls exceeded");
        emitter.send("event: error\ndata: " + objectMapper.writeValueAsString(error) + "\n\n");
    }

    private record StreamTurnResult(Map<Integer, ToolCallAccumulator> toolCalls, String assistantText) {
    }

    public static class ToolCallAccumulator {
        public String id;
        public String type = "function";
        public String name = "";
        public StringBuilder arguments = new StringBuilder();
    }
}
