package com.eqochat.business.actor.api.service;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.SubjectRef;

import java.util.Collection;
import java.util.Map;

public interface SubjectDirectoryApi {

    SubjectSummaryResponse getSubject(SubjectRef ref);

    Map<SubjectRef, SubjectSummaryResponse> batchGetSubjects(Collection<SubjectRef> refs);

    Long requireLiableHumanId(SubjectRef ref);
}
