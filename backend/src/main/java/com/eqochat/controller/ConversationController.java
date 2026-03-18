package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import com.eqochat.common.UserContext;
import com.eqochat.dto.request.CreateConversationRequest;
import com.eqochat.dto.request.SendMessageRequest;
import com.eqochat.dto.response.ConversationSummaryResponse;
import com.eqochat.dto.response.MessageResponse;
import com.eqochat.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {
    
    private final ConversationService conversationService;
    
    @GetMapping
    public ApiResponse<List<ConversationSummaryResponse>> listConversations() {
        return ApiResponse.success(conversationService.listConversations(UserContext.requireCurrentUser()));
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationSummaryResponse> getConversation(@PathVariable Long conversationId) {
        return ApiResponse.success(conversationService.getConversation(UserContext.requireCurrentUser(), conversationId));
    }
    
    @PostMapping
    public ApiResponse<ConversationSummaryResponse> createConversation(
            @RequestBody @Valid CreateConversationRequest request) {
        return ApiResponse.success(conversationService.createConversation(UserContext.requireCurrentUser(), request));
    }
    
    @GetMapping("/{conversationId}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(conversationService.getMessages(
                UserContext.requireCurrentUser(), conversationId, lastMessageId, limit));
    }

    @PostMapping("/{conversationId}/messages")
    public ApiResponse<MessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody @Valid SendMessageRequest request) {
        return ApiResponse.success(conversationService.sendMessage(
                UserContext.requireCurrentUser(), conversationId, request));
    }
}
