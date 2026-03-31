package com.eqochat.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateWorldPostRequest {

    @Size(max = 8000)
    private String content;

    /**
     * TEXT | IMAGE | VIDEO
     */
    @Size(max = 20)
    private String mediaType;

    @Size(max = 1024)
    private String imageUrl;

    @Size(max = 1024)
    private String videoUrl;

    @Size(max = 50)
    private List<Long> mentionedUserIds;
}
