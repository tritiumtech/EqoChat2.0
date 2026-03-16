package com.eqochat.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContactResponse {
    
    private Long id;
    private String nickname;
    private String avatarUrl;
    private String status;
}
