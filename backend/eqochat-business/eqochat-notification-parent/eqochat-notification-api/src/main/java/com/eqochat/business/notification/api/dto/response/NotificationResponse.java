package com.eqochat.business.notification.api.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String content;
    private boolean read;
    private LocalDateTime createTime;
}

