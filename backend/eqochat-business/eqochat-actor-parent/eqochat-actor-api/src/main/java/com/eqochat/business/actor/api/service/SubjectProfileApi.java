package com.eqochat.business.actor.api.service;

import com.eqochat.business.actor.api.dto.response.SubjectPublicProfileResponse;
import com.eqochat.business.actor.api.dto.response.SubjectSearchResponse;
import com.eqochat.business.actor.api.model.SubjectType;

import java.util.List;

public interface SubjectProfileApi {

    List<SubjectSearchResponse> search(
            Long principalHumanId,
            String keyword,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    );

    SubjectPublicProfileResponse getPublicProfile(
            Long principalHumanId,
            SubjectType subjectType,
            Long subjectId,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    );
}
