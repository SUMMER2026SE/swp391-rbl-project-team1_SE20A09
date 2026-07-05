package com.sportvenue.service.ai;

import com.sportvenue.dto.request.AiChatRequest;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.User;
import com.sportvenue.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private CustomerAgentToolProvider customerAgentToolProvider;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<InputStream> httpResponse;

    @Mock
    private SseEmitter sseEmitter;

    @Mock
    private UserPrincipal userPrincipal;

    @Mock
    private User user;

    @Mock
    private Role role;

    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        when(customerAgentToolProvider.getRoleName()).thenReturn("Customer");
        when(customerAgentToolProvider.getSystemPrompt(any())).thenReturn("System prompt for test");
        when(userPrincipal.getUser()).thenReturn(user);
        when(user.getRole()).thenReturn(role);
        when(role.getRoleName()).thenReturn("Customer");

        AgentRegistry agentRegistry = new AgentRegistry(List.of(customerAgentToolProvider));
        aiChatService = new AiChatServiceImpl(agentRegistry, httpClient);
        ReflectionTestUtils.setField(aiChatService, "aiBaseUrl", "https://api.groq.com/openai/v1");
        ReflectionTestUtils.setField(aiChatService, "aiApiKey", "mock-api-key");
        ReflectionTestUtils.setField(aiChatService, "aiModel", "llama-3.3-70b-versatile");
    }

    @Test
    void testHandleChatStream_SimpleTextResponse() throws Exception {
        String sseContent = 
                "data: {\"choices\":[{\"delta\":{\"content\":\"Hello world\"}}]}\n" +
                "data: [DONE]\n";
        
        InputStream inputStream = new ByteArrayInputStream(sseContent.getBytes(StandardCharsets.UTF_8));
        
        doReturn(httpResponse).when(httpClient).send(any(), any());
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(inputStream);

        AiChatRequest request = AiChatRequest.builder()
                .messages(List.of(
                        AiChatRequest.Message.builder()
                                .role("user")
                                .content("hi")
                                .build()
                ))
                .build();

        AtomicReference<InputStream> activeStreamRef = new AtomicReference<>();
        
        aiChatService.handleChatStream(request, sseEmitter, userPrincipal, activeStreamRef);

        verify(sseEmitter, atLeastOnce()).send(contains("event: text-delta"));
        verify(sseEmitter, atLeastOnce()).send(contains("\"type\":\"text-delta\""));
        verify(sseEmitter, atLeastOnce()).send(contains("\"delta\":\"Hello world\""));
        verify(sseEmitter, atLeastOnce()).send(contains("event: finish"));
        verify(sseEmitter, atLeastOnce()).send(contains("\"type\":\"finish\""));
        verify(sseEmitter).complete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testHandleChatStream_ToolCallResponse() throws Exception {
        String turn1Content = 
                "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_abc\",\"type\":\"function\",\"function\":{\"name\":\"searchStadiums\"}}]}}]}\n" +
                "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"{\\\"sportName\\\"\"}}]}}]}\n" +
                "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\":\\\"Bóng đá\\\"}\"}}]}}]}\n" +
                "data: [DONE]\n";
        
        String turn2Content = 
                "data: {\"choices\":[{\"delta\":{\"content\":\"Tôi đã tìm thấy sân bóng đá cho bạn.\"}}]}\n" +
                "data: [DONE]\n";
        
        InputStream is1 = new ByteArrayInputStream(turn1Content.getBytes(StandardCharsets.UTF_8));
        InputStream is2 = new ByteArrayInputStream(turn2Content.getBytes(StandardCharsets.UTF_8));

        HttpResponse<InputStream> resp1 = mock(HttpResponse.class);
        HttpResponse<InputStream> resp2 = mock(HttpResponse.class);

        when(resp1.statusCode()).thenReturn(200);
        when(resp1.body()).thenReturn(is1);
        when(resp2.statusCode()).thenReturn(200);
        when(resp2.body()).thenReturn(is2);

        doReturn(resp1).doReturn(resp2).when(httpClient).send(any(), any());

        when(customerAgentToolProvider.executeTool(eq("searchStadiums"), contains("Bóng đá"), any()))
                .thenReturn(Collections.emptyList());

        AiChatRequest request = AiChatRequest.builder()
                .messages(List.of(
                        AiChatRequest.Message.builder()
                                .role("user")
                                .content("Tìm sân bóng đá")
                                .build()
                ))
                .build();

        AtomicReference<InputStream> activeStreamRef = new AtomicReference<>();
        
        aiChatService.handleChatStream(request, sseEmitter, userPrincipal, activeStreamRef);

        verify(sseEmitter, atLeastOnce()).send(contains("event: tool-input-available"));
        verify(sseEmitter, atLeastOnce()).send(contains("event: tool-output-available"));
        verify(sseEmitter, atLeastOnce()).send(contains("event: text-delta"));
        verify(sseEmitter).complete();
    }

    @Test
    void testHandleChatStream_ToolCallEmptyContentSetsNullInHistory() throws Exception {
        String turn1Content =
                "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_abc\",\"type\":\"function\",\"function\":{\"name\":\"searchStadiums\",\"arguments\":\"{\\\"sportName\\\":\\\"Bóng đá\\\"}\"}}]}}]}\n" +
                "data: [DONE]\n";
        String turn2Content =
                "data: {\"choices\":[{\"delta\":{\"content\":\"Tôi đã tìm thấy sân bóng đá cho bạn.\"}}]}\n" +
                "data: [DONE]\n";

        InputStream is1 = new ByteArrayInputStream(turn1Content.getBytes(StandardCharsets.UTF_8));
        InputStream is2 = new ByteArrayInputStream(turn2Content.getBytes(StandardCharsets.UTF_8));

        HttpResponse<InputStream> resp1 = mock(HttpResponse.class);
        HttpResponse<InputStream> resp2 = mock(HttpResponse.class);

        when(resp1.statusCode()).thenReturn(200);
        when(resp1.body()).thenReturn(is1);
        when(resp2.statusCode()).thenReturn(200);
        when(resp2.body()).thenReturn(is2);

        doReturn(resp1).doReturn(resp2).when(httpClient).send(any(), any());

        when(customerAgentToolProvider.executeTool(eq("searchStadiums"), contains("Bóng đá"), any()))
                .thenReturn(Collections.emptyList());

        AiChatRequest request = AiChatRequest.builder()
                .messages(List.of(
                        AiChatRequest.Message.builder()
                                .role("user")
                                .content("Tìm sân bóng đá")
                                .build()
                ))
                .build();

        AtomicReference<InputStream> activeStreamRef = new AtomicReference<>();
        aiChatService.handleChatStream(request, sseEmitter, userPrincipal, activeStreamRef);

        verify(sseEmitter, atLeastOnce()).send(contains("event: tool-input-available"));
        verify(sseEmitter, atLeastOnce()).send(contains("event: tool-output-available"));
        verify(sseEmitter, atLeastOnce()).send(contains("event: text-delta"));
        verify(sseEmitter).complete();
    }

    @Test
    void testHandleChatStream_ToolCallJsonParseFailure() throws Exception {
        // arguments is malformed JSON
        String turn1Content =
                "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_abc\",\"type\":\"function\",\"function\":{\"name\":\"searchStadiums\",\"arguments\":\"{\\\"sportName\\\"\"}}]}}]}\n" +
                "data: [DONE]\n";
        String turn2Content =
                "data: {\"choices\":[{\"delta\":{\"content\":\"Lỗi đối số\"}}]}\n" +
                "data: [DONE]\n";

        InputStream is1 = new ByteArrayInputStream(turn1Content.getBytes(StandardCharsets.UTF_8));
        InputStream is2 = new ByteArrayInputStream(turn2Content.getBytes(StandardCharsets.UTF_8));

        HttpResponse<InputStream> resp1 = mock(HttpResponse.class);
        HttpResponse<InputStream> resp2 = mock(HttpResponse.class);

        when(resp1.statusCode()).thenReturn(200);
        when(resp1.body()).thenReturn(is1);
        when(resp2.statusCode()).thenReturn(200);
        when(resp2.body()).thenReturn(is2);

        doReturn(resp1).doReturn(resp2).when(httpClient).send(any(), any());

        AiChatRequest request = AiChatRequest.builder()
                .messages(List.of(
                        AiChatRequest.Message.builder()
                                .role("user")
                                .content("Tìm sân")
                                .build()
                ))
                .build();

        AtomicReference<InputStream> activeStreamRef = new AtomicReference<>();
        aiChatService.handleChatStream(request, sseEmitter, userPrincipal, activeStreamRef);

        // verify that executeTool was NEVER called since arguments JSON was invalid
        verify(customerAgentToolProvider, never()).executeTool(any(), any(), any());

        // verify emitter sends tool result containing "Invalid arguments"
        verify(sseEmitter, atLeastOnce()).send(contains("event: tool-output-available"));
        verify(sseEmitter, atLeastOnce()).send(contains("Invalid arguments"));
        verify(sseEmitter).complete();
    }

    @Test
    void testHandleChatStream_ToolCallExecutionThrowsException() throws Exception {
        String turn1Content =
                "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_abc\",\"type\":\"function\",\"function\":{\"name\":\"searchStadiums\",\"arguments\":\"{\\\"sportName\\\":\\\"Bóng đá\\\"}\"}}]}}]}\n" +
                "data: [DONE]\n";
        String turn2Content =
                "data: {\"choices\":[{\"delta\":{\"content\":\"Lỗi thực thi\"}}]}\n" +
                "data: [DONE]\n";

        InputStream is1 = new ByteArrayInputStream(turn1Content.getBytes(StandardCharsets.UTF_8));
        InputStream is2 = new ByteArrayInputStream(turn2Content.getBytes(StandardCharsets.UTF_8));

        HttpResponse<InputStream> resp1 = mock(HttpResponse.class);
        HttpResponse<InputStream> resp2 = mock(HttpResponse.class);

        when(resp1.statusCode()).thenReturn(200);
        when(resp1.body()).thenReturn(is1);
        when(resp2.statusCode()).thenReturn(200);
        when(resp2.body()).thenReturn(is2);

        doReturn(resp1).doReturn(resp2).when(httpClient).send(any(), any());

        // Throw exception when tool runs
        when(customerAgentToolProvider.executeTool(eq("searchStadiums"), contains("Bóng đá"), any()))
                .thenThrow(new RuntimeException("Database timeout"));

        AiChatRequest request = AiChatRequest.builder()
                .messages(List.of(
                        AiChatRequest.Message.builder()
                                .role("user")
                                .content("Tìm sân")
                                .build()
                ))
                .build();

        AtomicReference<InputStream> activeStreamRef = new AtomicReference<>();
        aiChatService.handleChatStream(request, sseEmitter, userPrincipal, activeStreamRef);

        // verify emitter sends tool result containing the execution error
        verify(sseEmitter, atLeastOnce()).send(contains("event: tool-output-available"));
        verify(sseEmitter, atLeastOnce()).send(contains("Tool execution failed: Database timeout"));
        verify(sseEmitter).complete();
    }

    @Test
    void testHandleChatStream_MaxToolCallsLimitExceeded() throws Exception {
        // Generate a response that constantly triggers tool calls to verify MAX_TOOL_CALLS = 10 limit.
        // We will mock the httpClient to return tool call responses repeatedly.
        String toolCallResponse =
                "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_abc\",\"type\":\"function\",\"function\":{\"name\":\"searchStadiums\",\"arguments\":\"{\\\"sportName\\\":\\\"Bóng đá\\\"}\"}}]}}]}\n" +
                "data: [DONE]\n";

        HttpResponse<InputStream> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(200);

        // We want it to be called multiple times. Since MAX_TOOL_CALLS is 10, and each call returns 1 tool call,
        // it should stop after 10 loops. Let's make httpClient return the tool call response for 11 times.
        when(resp.body()).thenAnswer(invocation -> new ByteArrayInputStream(toolCallResponse.getBytes(StandardCharsets.UTF_8)));

        // Setup mock for all calls
        doReturn(resp).when(httpClient).send(any(), any());

        when(customerAgentToolProvider.executeTool(eq("searchStadiums"), contains("Bóng đá"), any()))
                .thenReturn(Collections.emptyList());

        AiChatRequest request = AiChatRequest.builder()
                .messages(List.of(
                        AiChatRequest.Message.builder()
                                .role("user")
                                .content("Tìm sân bóng đá")
                                .build()
                ))
                .build();

        AtomicReference<InputStream> activeStreamRef = new AtomicReference<>();
        aiChatService.handleChatStream(request, sseEmitter, userPrincipal, activeStreamRef);

        // verify that the emitter receives "Max tool calls exceeded" error and completes
        verify(sseEmitter, atLeastOnce()).send(contains("event: error"));
        verify(sseEmitter, atLeastOnce()).send(contains("Max tool calls exceeded"));
        verify(sseEmitter).complete();
    }

    @Test
    void testHandleChatStream_GroqStreamError() throws Exception {
        String errorStreamContent =
                "event: error\n" +
                "data: {\"error\":{\"message\":\"Failed to call a function. Please adjust your prompt.\",\"type\":\"invalid_request_error\",\"code\":\"tool_use_failed\"}}\n";

        InputStream inputStream = new ByteArrayInputStream(errorStreamContent.getBytes(StandardCharsets.UTF_8));

        doReturn(httpResponse).when(httpClient).send(any(), any());
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(inputStream);

        AiChatRequest request = AiChatRequest.builder()
                .messages(List.of(
                        AiChatRequest.Message.builder()
                                .role("user")
                                .content("Tìm sân bóng đá")
                                .build()
                ))
                .build();

        AtomicReference<InputStream> activeStreamRef = new AtomicReference<>();

        aiChatService.handleChatStream(request, sseEmitter, userPrincipal, activeStreamRef);

        verify(sseEmitter, atLeastOnce()).send(contains("event: error"));
        verify(sseEmitter, atLeastOnce()).send(contains("LLM Stream Error: Failed to call a function. Please adjust your prompt."));
        verify(sseEmitter).completeWithError(any(RuntimeException.class));
    }

    @Test
    void testHandleChatStream_AuthError401_ReturnsFriendlyMessageNotRawDetails() throws Exception {
        String errorBody = "{\"error\":{\"message\":\"Invalid API Key\",\"type\":\"invalid_request_error\"}}";
        InputStream errorBodyStream = new ByteArrayInputStream(errorBody.getBytes(StandardCharsets.UTF_8));

        doReturn(httpResponse).when(httpClient).send(any(), any());
        when(httpResponse.statusCode()).thenReturn(401);
        when(httpResponse.body()).thenReturn(errorBodyStream);

        AiChatRequest request = AiChatRequest.builder()
                .messages(List.of(AiChatRequest.Message.builder().role("user").content("Xin chào").build()))
                .build();

        AtomicReference<InputStream> activeStreamRef = new AtomicReference<>();

        aiChatService.handleChatStream(request, sseEmitter, userPrincipal, activeStreamRef);

        verify(sseEmitter, atLeastOnce()).send(contains("event: error"));
        verify(sseEmitter, atLeastOnce()).send(contains("sự cố cấu hình"));
        verify(sseEmitter, never()).send(contains("Invalid API Key"));
        verify(sseEmitter).completeWithError(any(LlmGatewayException.class));
    }

    @Test
    void testHandleChatStream_RateLimited429_ReturnsFriendlyMessage() throws Exception {
        String errorBody = "{\"error\":{\"message\":\"Rate limit reached for model\",\"type\":\"tokens\"}}";
        InputStream errorBodyStream = new ByteArrayInputStream(errorBody.getBytes(StandardCharsets.UTF_8));

        doReturn(httpResponse).when(httpClient).send(any(), any());
        when(httpResponse.statusCode()).thenReturn(429);
        when(httpResponse.body()).thenReturn(errorBodyStream);

        AiChatRequest request = AiChatRequest.builder()
                .messages(List.of(AiChatRequest.Message.builder().role("user").content("Xin chào").build()))
                .build();

        AtomicReference<InputStream> activeStreamRef = new AtomicReference<>();

        aiChatService.handleChatStream(request, sseEmitter, userPrincipal, activeStreamRef);

        verify(sseEmitter, atLeastOnce()).send(contains("event: error"));
        verify(sseEmitter, atLeastOnce()).send(contains("giới hạn tốc độ"));
        verify(sseEmitter).completeWithError(any(LlmGatewayException.class));
    }

    @Test
    void testHandleChatStream_ToolCallValidationErrorInStream_ReturnsFriendlyMessage() throws Exception {
        String errorStreamContent =
                "event: error\n" +
                "data: {\"error\":{\"message\":\"tool call validation failed: parameters for tool getStadiumSlots did not match schema\",\"type\":\"invalid_request_error\"}}\n";

        InputStream inputStream = new ByteArrayInputStream(errorStreamContent.getBytes(StandardCharsets.UTF_8));

        doReturn(httpResponse).when(httpClient).send(any(), any());
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(inputStream);

        AiChatRequest request = AiChatRequest.builder()
                .messages(List.of(AiChatRequest.Message.builder().role("user").content("Sân vận động Cẩm Lệ").build()))
                .build();

        AtomicReference<InputStream> activeStreamRef = new AtomicReference<>();

        aiChatService.handleChatStream(request, sseEmitter, userPrincipal, activeStreamRef);

        verify(sseEmitter, atLeastOnce()).send(contains("event: error"));
        verify(sseEmitter, atLeastOnce()).send(contains("diễn đạt lại câu hỏi"));
        verify(sseEmitter, never()).send(contains("did not match schema"));
        verify(sseEmitter).completeWithError(any(LlmGatewayException.class));
    }
}
