package com.eqochat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWorldPostReplyRequest {

    @NotBlank
    @Size(max = 8000)
    private String content;
}

