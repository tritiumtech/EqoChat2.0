package com.eqochat.business.notification.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarkNotificationReadRequest {
    @NotNull
    private Long notificationId;
}

