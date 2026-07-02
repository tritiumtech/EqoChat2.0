package com.eqochat.business.contact.controller.friend;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.contact.api.dto.request.UpdateContactTagsRequest;
import com.eqochat.business.contact.api.dto.response.ContactDetailResponse;
import com.eqochat.business.contact.api.dto.response.ContactResponse;
import com.eqochat.business.contact.api.service.ContactService;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.common.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactControllerOwnerSubjectTest {

    @Mock
    ContactService contactService;

    ContactController controller;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(9L);
        controller = new ContactController(contactService);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void listContactsRejectsMissingOwnerSubjectBeforeServiceCall() {
        assertThatThrownBy(() -> controller.listContacts(null, null, null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("contact.subject.invalid");

        verifyNoInteractions(contactService);
    }

    @Test
    void listContactsPassesExplicitAgentOwnerSubject() {
        when(contactService.listContacts(9L, SubjectRef.agent(101L))).thenReturn(List.of(
                ContactResponse.builder()
                        .ownerSubjectId(101L)
                        .ownerSubjectType(SubjectType.AGENT)
                        .targetSubjectId(2L)
                        .targetSubjectType(SubjectType.HUMAN)
                        .nickname("Ava")
                        .build()
        ));

        var response = controller.listContacts(null, null, 101L, SubjectType.AGENT).getData();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getOwnerSubjectType()).isEqualTo(SubjectType.AGENT);
        verify(contactService).listContacts(9L, SubjectRef.agent(101L));
    }

    @Test
    void detailAndTagsPassExplicitOwnerSubject() {
        when(contactService.getContactDetail(9L, SubjectRef.agent(101L), SubjectRef.human(2L)))
                .thenReturn(ContactDetailResponse.builder()
                        .ownerSubjectId(101L)
                        .ownerSubjectType(SubjectType.AGENT)
                        .targetSubjectId(2L)
                        .targetSubjectType(SubjectType.HUMAN)
                        .nickname("Ava")
                        .build());
        when(contactService.updateContactTags(9L, SubjectRef.agent(101L), SubjectRef.human(2L), List.of("team")))
                .thenReturn(List.of("team"));

        controller.getContactDetail(SubjectType.HUMAN, 2L, 101L, SubjectType.AGENT);

        UpdateContactTagsRequest request = new UpdateContactTagsRequest();
        request.setTags(List.of("team"));
        controller.updateContactTags(SubjectType.HUMAN, 2L, 101L, SubjectType.AGENT, request);

        verify(contactService).getContactDetail(9L, SubjectRef.agent(101L), SubjectRef.human(2L));
        ArgumentCaptor<List<String>> tags = ArgumentCaptor.forClass(List.class);
        verify(contactService).updateContactTags(
                org.mockito.Mockito.eq(9L),
                org.mockito.Mockito.eq(SubjectRef.agent(101L)),
                org.mockito.Mockito.eq(SubjectRef.human(2L)),
                tags.capture());
        assertThat(tags.getValue()).containsExactly("team");
    }

    @Test
    void partialOwnerSubjectParamsAreRejectedBeforeServiceCall() {
        assertThatThrownBy(() -> controller.listContacts(null, null, 101L, null))
                .isInstanceOf(BizException.class)
                .hasMessage("contact.subject.invalid");

        assertThatThrownBy(() -> controller.getContactDetail(SubjectType.HUMAN, 2L, null, SubjectType.AGENT))
                .isInstanceOf(BizException.class)
                .hasMessage("contact.subject.invalid");

        assertThatThrownBy(() -> controller.updateContactTags(SubjectType.HUMAN, 2L, null, null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("contact.subject.invalid");

        verifyNoInteractions(contactService);
    }

    @Test
    void systemOwnerSubjectIsRejectedBeforeServiceCall() {
        assertThatThrownBy(() -> controller.listContacts(null, null, 0L, SubjectType.SYSTEM))
                .isInstanceOf(BizException.class)
                .hasMessage("contact.subject.invalid");

        verifyNoInteractions(contactService);
    }
}
