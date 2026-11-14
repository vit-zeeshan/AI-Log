package com.loganalyzer.controller;

import com.loganalyzer.ai.AiChatService;
import com.loganalyzer.dto.ApiResponse;
import com.loganalyzer.dto.ChatDtos;
import com.loganalyzer.entity.User;
import com.loganalyzer.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Assistant", description = "Endpoints for context-aware SRE AI chat assistant")
public class AiController {

    private final AiChatService aiChatService;
    private final AuthService authService;

    public AiController(AiChatService aiChatService, AuthService authService) {
        this.aiChatService = aiChatService;
        this.authService = authService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Ask question to AI Log Assistant based on actual log evidence")
    public ResponseEntity<ApiResponse<ChatDtos.ChatResponse>> askAssistant(
            @RequestBody ChatDtos.ChatRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        ChatDtos.ChatResponse response = aiChatService.processMessage(request, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
