package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.model.SubjectRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubjectRegistrySyncServiceImplTest {

    @Mock
    SubjectDirectoryServiceImpl subjectDirectoryService;

    @Mock
    SubjectRegistryRepository subjectRegistryRepository;

    @Test
    void syncUsesForcedSourceRefreshRatherThanRegistryHit() {
        SubjectRegistrySyncServiceImpl service =
                new SubjectRegistrySyncServiceImpl(subjectDirectoryService, subjectRegistryRepository);

        service.syncHuman(2L);
        service.syncAgent(101L);

        verify(subjectDirectoryService).refreshHuman(2L);
        verify(subjectDirectoryService).refreshAgent(101L);
    }

    @Test
    void retireSoftDeletesRegistryRowsByCanonicalSubjectRef() {
        SubjectRegistrySyncServiceImpl service =
                new SubjectRegistrySyncServiceImpl(subjectDirectoryService, subjectRegistryRepository);

        service.retireHuman(2L);
        service.retireAgent(101L);

        verify(subjectRegistryRepository).retire(SubjectRef.human(2L));
        verify(subjectRegistryRepository).retire(SubjectRef.agent(101L));
    }
}
