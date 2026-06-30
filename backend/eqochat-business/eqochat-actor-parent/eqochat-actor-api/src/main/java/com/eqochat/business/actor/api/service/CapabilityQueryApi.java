package com.eqochat.business.actor.api.service;

import com.eqochat.business.actor.api.model.Capability;
import com.eqochat.business.actor.api.model.CapabilityCode;
import com.eqochat.business.actor.api.model.CapabilitySet;
import com.eqochat.business.actor.api.model.SubjectRef;

public interface CapabilityQueryApi {

    Capability getCapability(SubjectRef ref, CapabilityCode code);

    CapabilitySet getCapabilities(SubjectRef ref);
}
