package com.eqochat.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateContactTagsRequest {

    @Size(max = 20)
    private List<@Size(max = 24) String> tags;
}
