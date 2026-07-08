package com.sportvenue.service.ai;

import com.sportvenue.dto.request.AiChatTurnRequest;
import com.sportvenue.entity.AiUsageLog;
import com.sportvenue.repository.AiUsageLogRepository;
import com.sportvenue.service.ai.handler.MatchRequestHandler;
import com.sportvenue.service.ai.handler.PolicyHandler;
import com.sportvenue.service.ai.handler.SlotAvailabilityHandler;
import com.sportvenue.service.ai.handler.StadiumSearchHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplLogTest {

    @Mock
    private GroqClient groqClient;
    @Mock
    private StadiumSearchHandler stadiumSearchHandler;
    @Mock
    private SlotAvailabilityHandler slotAvailabilityHandler;
    @Mock
    private MatchRequestHandler matchRequestHandler;
    @Mock
    private PolicyHandler policyHandler;
    @Mock
    private AiUsageLogRepository aiUsageLogRepository;

    private AiChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiChatServiceImpl(groqClient, stadiumSearchHandler, slotAvailabilityHandler,
                matchRequestHandler, policyHandler, aiUsageLogRepository);
    }

    private AiChatTurnRequest request(String message) {
        return AiChatTurnRequest.builder().message(message).build();
    }

    @Test
    void happyPath_savesUsageLogWithCorrectFields() {
        when(groqClient.chatJson(any(), any(), any(), any()))
                .thenReturn(new GroqClient.GroqResult(
                        "{\"intent\":\"search_stadiums\",\"message\":\"Ok\",\"params\":{}}",
                        120, 80, 200));

        service.handleChat(request("Tìm sân bóng đá"), null, "s:test");

        ArgumentCaptor<AiUsageLog> logCaptor = ArgumentCaptor.forClass(AiUsageLog.class);
        verify(aiUsageLogRepository).save(logCaptor.capture());

        AiUsageLog saved = logCaptor.getValue();
        assertThat(saved.getFeature()).isEqualTo("search_stadiums");
        assertThat(saved.getInputTokens()).isEqualTo(120);
        assertThat(saved.getOutputTokens()).isEqualTo(80);
        assertThat(saved.getLatencyMs()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void jsonParseFail_stillSavesUsageLogAsUnknown() {
        when(groqClient.chatJson(any(), any(), any(), any()))
                .thenReturn(new GroqClient.GroqResult(
                        "khong-phai-json-hop-le",
                        100, 50, 150));

        service.handleChat(request("Tìm sân bóng đá"), null, "s:test");

        ArgumentCaptor<AiUsageLog> logCaptor = ArgumentCaptor.forClass(AiUsageLog.class);
        verify(aiUsageLogRepository).save(logCaptor.capture());

        AiUsageLog saved = logCaptor.getValue();
        assertThat(saved.getFeature()).isEqualTo("unknown");
        assertThat(saved.getInputTokens()).isEqualTo(100);
        assertThat(saved.getOutputTokens()).isEqualTo(50);
    }

    @Test
    void llmGatewayException_doesNotSaveUsageLog() {
        when(groqClient.chatJson(any(), any(), any(), any()))
                .thenThrow(new LlmGatewayException(LlmGatewayException.Kind.RATE_LIMITED, "rate limited"));

        service.handleChat(request("Tìm sân bóng đá"), null, "s:test");

        verify(aiUsageLogRepository, never()).save(any());
    }
}
