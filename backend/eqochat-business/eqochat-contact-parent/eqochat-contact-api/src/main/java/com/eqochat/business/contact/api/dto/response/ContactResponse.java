package com.eqochat.business.contact.api.dto.response;

import com.eqochat.business.actor.api.model.SubjectType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ContactResponse {

    private Long ownerSubjectId;
    private SubjectType ownerSubjectType;
    private Long targetSubjectId;
    private SubjectType targetSubjectType;
    private String nickname;
    private String avatarUrl;
    private String status;
    private List<String> tags;
}
