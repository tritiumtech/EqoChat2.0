package com.eqochat.business.notification.api.dto.request;

import com.eqochat.business.actor.api.model.SubjectType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarkNotificationReadRequest {
    @NotNull
    private Long notificationId;
    @NotNull
    private Long recipientSubjectId;
    @NotNull
    private SubjectType recipientSubjectType;
}
