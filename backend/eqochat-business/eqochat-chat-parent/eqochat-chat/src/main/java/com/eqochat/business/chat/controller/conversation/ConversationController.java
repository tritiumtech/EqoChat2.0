package com.eqochat.business.chat.controller.conversation;

import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.common.PageResponse;
import com.eqochat.framework.common.UserContext;
import com.eqochat.business.chat.api.dto.request.CreateConversationRequest;
import com.eqochat.business.chat.api.dto.request.MarkConversationReadRequest;
import com.eqochat.business.chat.api.dto.request.SendMessageRequest;
import com.eqochat.business.chat.api.dto.response.ConversationSummaryResponse;
import com.eqochat.business.chat.api.dto.response.MessageResponse;
import com.eqochat.business.chat.api.service.ConversationService;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {
    
    private final ConversationService conversationService;
    private final ConversationParticipantService conversationParticipantService;
    
    @GetMapping
    public ApiResponse<List<ConversationSummaryResponse>> listConversations(
            @RequestParam(required = false) String q) {
        List<ConversationSummaryResponse> list = conversationService.listConversations(UserContext.requireCurrentUser());
        if (q == null || q.isBlank()) {
            return ApiResponse.success(list);
        }
        String keyword = q.trim().toLowerCase();
        return ApiResponse.success(list.stream()
                .filter(item -> {
                    String title = item.getTitle() != null ? item.getTitle() : "";
                    String last = item.getLastMessage() != null ? item.getLastMessage() : "";
                    return title.toLowerCase().contains(keyword) || last.toLowerCase().contains(keyword);
                })
                .toList());
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
    public ApiResponse<PageResponse<MessageResponse>> getMessages(
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

    @PostMapping("/{conversationId}/read")
    public ApiResponse<Void> markRead(@PathVariable Long conversationId,
                                      @RequestBody @Valid MarkConversationReadRequest request) {
        conversationParticipantService.updateLastRead(
                conversationId,
                UserContext.requireCurrentUser(),
                request.getMessageId(),
                LocalDateTime.now()
        );
        return ApiResponse.success(null);
    }
}
