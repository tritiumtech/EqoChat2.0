package com.eqochat.business.actor.api.service;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.SubjectRef;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SubjectDirectoryApi {

    SubjectSummaryResponse getSubject(SubjectRef ref);

    Map<SubjectRef, SubjectSummaryResponse> batchGetSubjects(Collection<SubjectRef> refs);

    /**
     * Returns subjects the authenticated human principal may explicitly represent.
     *
     * The principal's own HUMAN subject is returned only when it exists in the
     * subject registry; owned or otherwise delegated subjects also come from the
     * registry. Callers must still pass one of these subjects explicitly as
     * viewer/actor/owner identity instead of deriving a business subject from the
     * auth principal.
     */
    List<SubjectRef> listAssociatedSubjects(Long principalHumanId);

    Long requireLiableHumanId(SubjectRef ref);
}
