package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import com.eqochat.common.BizException;
import com.eqochat.dto.request.CreateConversationRequest;
import com.eqochat.dto.request.SendMessageRequest;
import com.eqochat.dto.response.ConversationSummaryResponse;
import com.eqochat.dto.response.MessageResponse;
import com.eqochat.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Long userId = resolveUserId();
        return ApiResponse.success(conversationService.listConversations(userId));
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationSummaryResponse> getConversation(@PathVariable Long conversationId) {
        Long userId = resolveUserId();
        return ApiResponse.success(conversationService.getConversation(userId, conversationId));
    }
    
    @PostMapping
    public ApiResponse<ConversationSummaryResponse> createConversation(
            @RequestBody @Valid CreateConversationRequest request) {
        Long userId = resolveUserId();
        return ApiResponse.success(conversationService.createConversation(userId, request));
    }
    
    @GetMapping("/{conversationId}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(required = false) Integer limit) {
        Long userId = resolveUserId();
        return ApiResponse.success(conversationService.getMessages(userId, conversationId, lastMessageId, limit));
    }

    @PostMapping("/{conversationId}/messages")
    public ApiResponse<MessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody @Valid SendMessageRequest request) {
        Long userId = resolveUserId();
        return ApiResponse.success(conversationService.sendMessage(userId, conversationId, request));
    }
    
    private Long resolveUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw BizException.of(401, "auth.token.invalid");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        throw BizException.of(401, "auth.token.invalid");
    }
}
