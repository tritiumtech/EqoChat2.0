package com.eqochat.business.actor.api.service;

import com.eqochat.business.actor.api.model.MilestoneBenefit;
import com.eqochat.business.actor.api.model.SubjectRef;

public interface MilestonePolicyApi {

    MilestoneBenefit resolveMilestone(SubjectRef subject);
}
