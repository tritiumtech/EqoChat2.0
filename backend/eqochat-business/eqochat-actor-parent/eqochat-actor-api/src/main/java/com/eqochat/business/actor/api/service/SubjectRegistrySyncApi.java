package com.eqochat.business.actor.api.service;

public interface SubjectRegistrySyncApi {

    void syncHuman(Long humanId);

    void syncAgent(Long agentId);

    void retireHuman(Long humanId);

    void retireAgent(Long agentId);
}
