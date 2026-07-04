package com.sportvenue.service.ai;

import com.sportvenue.dto.request.AiChatRequest;
import com.sportvenue.security.UserPrincipal;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

public interface AiChatService {
    void handleChatStream(AiChatRequest request, ResponseBodyEmitter emitter, UserPrincipal userPrincipal,
                           AtomicReference<InputStream> activeStreamRef);
}
