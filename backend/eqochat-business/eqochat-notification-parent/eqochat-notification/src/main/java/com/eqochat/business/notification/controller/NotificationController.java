package com.eqochat.business.notification.controller;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.common.UserContext;
import com.eqochat.business.notification.api.dto.request.MarkNotificationReadRequest;
import com.eqochat.business.notification.api.dto.response.NotificationResponse;
import com.eqochat.business.notification.api.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final LiabilityPolicyApi liabilityPolicyApi;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> listMine(@RequestParam(required = false) Integer limit,
                                                            @RequestParam(required = false) Long recipientSubjectId,
                                                            @RequestParam(required = false) SubjectType recipientSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef recipient = resolveRecipient(recipientSubjectId, recipientSubjectType);
        requireAuthorizedRecipient(principalHumanId, recipient);
        return ApiResponse.success(notificationService.listNotifications(recipient, limit));
    }

    @PostMapping("/read")
    public ApiResponse<Void> markRead(@RequestBody @Valid MarkNotificationReadRequest request) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef recipient = resolveRecipient(
                request.getRecipientSubjectId(),
                request.getRecipientSubjectType());
        requireAuthorizedRecipient(principalHumanId, recipient);
        notificationService.markRead(recipient, request.getNotificationId());
        return ApiResponse.success(null);
    }

    private static SubjectRef resolveRecipient(Long recipientSubjectId, SubjectType recipientSubjectType) {
        if (recipientSubjectId == null || recipientSubjectType == null || recipientSubjectType == SubjectType.SYSTEM) {
            throw BizException.of("notification.recipient.type.invalid");
        }
        return new SubjectRef(recipientSubjectId, recipientSubjectType);
    }

    private void requireAuthorizedRecipient(Long principalHumanId, SubjectRef recipient) {
        var chain = liabilityPolicyApi.resolveLiability(recipient);
        Long liableHumanId = chain != null ? chain.liableHumanId() : null;
        if (!principalHumanId.equals(liableHumanId)) {
            throw BizException.of("notification.access.denied");
        }
    }
}
