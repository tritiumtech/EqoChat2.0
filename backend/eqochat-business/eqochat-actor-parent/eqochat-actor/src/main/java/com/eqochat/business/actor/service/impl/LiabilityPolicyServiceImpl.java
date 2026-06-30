package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LiabilityPolicyServiceImpl implements LiabilityPolicyApi {

    private final ActorSubjectValidator subjectValidator;

    @Override
    public LiabilityChain resolveLiability(SubjectRef actor) {
        if (actor == null) {
            return LiabilityChain.unresolved(null, "subject is null");
        }
        if (actor.isHuman()) {
            ActorSubjectValidation validation = subjectValidator.validateHuman(actor.id());
            return validation.valid()
                    ? LiabilityChain.selfResponsible(actor.id())
                    : LiabilityChain.unresolved(actor, validation.reason());
        }
        if (actor.isAgent()) {
            ActorSubjectValidation validation = subjectValidator.validateAgentLiability(actor.id());
            return validation.valid()
                    ? LiabilityChain.agentToHuman(validation.agent().getId(), validation.human().getId())
                    : LiabilityChain.unresolved(actor, validation.reason());
        }
        return LiabilityChain.unresolved(actor, "system subject has no human liability");
    }
}
