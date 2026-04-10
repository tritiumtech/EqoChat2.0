package com.eqochat.business.chat.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MessagePageResponse {

    private List<MessageResponse> items;
    private Long total;
    private Boolean hasMore;
    private Long nextLastMessageId;
}
