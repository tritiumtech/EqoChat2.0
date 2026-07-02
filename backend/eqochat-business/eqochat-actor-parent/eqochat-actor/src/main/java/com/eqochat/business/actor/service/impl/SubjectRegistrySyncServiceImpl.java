package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.service.SubjectRegistrySyncApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubjectRegistrySyncServiceImpl implements SubjectRegistrySyncApi {

    private final SubjectDirectoryServiceImpl subjectDirectoryService;
    private final SubjectRegistryRepository subjectRegistryRepository;

    @Override
    public void syncHuman(Long humanId) {
        if (humanId == null || humanId <= 0) {
            return;
        }
        subjectDirectoryService.refreshHuman(humanId);
    }

    @Override
    public void syncAgent(Long agentId) {
        if (agentId == null || agentId <= 0) {
            return;
        }
        subjectDirectoryService.refreshAgent(agentId);
    }

    @Override
    public void retireHuman(Long humanId) {
        if (humanId == null || humanId <= 0) {
            return;
        }
        subjectRegistryRepository.retire(SubjectRef.human(humanId));
    }

    @Override
    public void retireAgent(Long agentId) {
        if (agentId == null || agentId <= 0) {
            return;
        }
        subjectRegistryRepository.retire(SubjectRef.agent(agentId));
    }
}
