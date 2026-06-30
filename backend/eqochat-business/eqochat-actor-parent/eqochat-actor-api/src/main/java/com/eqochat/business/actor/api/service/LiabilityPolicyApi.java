package com.eqochat.business.actor.api.service;

import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;

public interface LiabilityPolicyApi {

    LiabilityChain resolveLiability(SubjectRef actor);
}
