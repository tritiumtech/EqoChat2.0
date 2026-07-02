package com.eqochat.business.actor.controller;

import com.eqochat.business.actor.api.dto.response.SubjectPublicProfileResponse;
import com.eqochat.business.actor.api.dto.response.SubjectSearchResponse;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.service.impl.SubjectProfileServiceImpl;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.common.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectControllerPublicProfileTest {

    @Mock
    SubjectProfileServiceImpl subjectProfileService;

    SubjectController controller;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(9L);
        controller = new SubjectController(subjectProfileService);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void searchReturnsCanonicalSubjectResults() {
        when(subjectProfileService.search(9L, "nova", 101L, SubjectType.AGENT)).thenReturn(List.of(
                SubjectSearchResponse.builder()
                        .subjectId(101L)
                        .subjectType(SubjectType.AGENT)
                        .displayName("Nova")
                        .build()
        ));

        var data = controller.search("nova", 101L, SubjectType.AGENT).getData();

        assertThat(data).hasSize(1);
        assertThat(data.getFirst().getSubjectType()).isEqualTo(SubjectType.AGENT);
        verify(subjectProfileService).search(9L, "nova", 101L, SubjectType.AGENT);
    }

    @Test
    void publicProfilePassesHumanAndAgentSubjectTypes() {
        when(subjectProfileService.getPublicProfile(9L, SubjectType.AGENT, 101L, 101L, SubjectType.AGENT)).thenReturn(
                SubjectPublicProfileResponse.builder()
                        .subjectId(101L)
                        .subjectType(SubjectType.AGENT)
                        .displayName("Nova")
                        .build()
        );

        var profile = controller.publicProfile("AGENT", 101L, 101L, SubjectType.AGENT).getData();

        assertThat(profile.getSubjectId()).isEqualTo(101L);
        assertThat(profile.getSubjectType()).isEqualTo(SubjectType.AGENT);
        verify(subjectProfileService).getPublicProfile(9L, SubjectType.AGENT, 101L, 101L, SubjectType.AGENT);
    }

    @Test
    void searchRejectsMissingViewerSubjectBeforeServiceCall() {
        assertThatThrownBy(() -> controller.search("nova", null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("subject.viewer.invalid");

        verifyNoMoreInteractions(subjectProfileService);
    }

    @Test
    void publicProfileRejectsSystemViewerSubjectBeforeServiceCall() {
        assertThatThrownBy(() -> controller.publicProfile("AGENT", 101L, 0L, SubjectType.SYSTEM))
                .isInstanceOf(BizException.class)
                .hasMessage("subject.viewer.invalid");

        verifyNoMoreInteractions(subjectProfileService);
    }

    @Test
    void systemAndLegacyUserSubjectTypesAreRejectedBeforeServiceCall() {
        assertThatThrownBy(() -> controller.publicProfile("SYSTEM", 0L, 9L, SubjectType.HUMAN))
                .isInstanceOf(BizException.class)
                .hasMessage("subject.type.invalid");

        assertThatThrownBy(() -> controller.publicProfile("USER", 101L, 9L, SubjectType.HUMAN))
                .isInstanceOf(BizException.class)
                .hasMessage("subject.type.invalid");

        verifyNoMoreInteractions(subjectProfileService);
    }
}
