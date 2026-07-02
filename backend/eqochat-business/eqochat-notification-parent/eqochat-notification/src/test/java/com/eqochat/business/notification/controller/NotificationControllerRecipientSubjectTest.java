package com.eqochat.business.notification.controller;

import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.notification.api.dto.request.MarkNotificationReadRequest;
import com.eqochat.business.notification.api.dto.response.NotificationResponse;
import com.eqochat.business.notification.api.service.NotificationService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerRecipientSubjectTest {

    @Mock
    NotificationService notificationService;
    @Mock
    LiabilityPolicyApi liabilityPolicyApi;

    NotificationController controller;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(9L);
        controller = new NotificationController(notificationService, liabilityPolicyApi);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void listRejectsMissingRecipientBeforePolicyCall() {
        assertThatThrownBy(() -> controller.listMine(20, null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("notification.recipient.type.invalid");

        verifyNoInteractions(liabilityPolicyApi, notificationService);
    }

    @Test
    void listPassesExplicitAgentRecipientWhenLiableToPrincipalHuman() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        when(notificationService.listNotifications(SubjectRef.agent(101L), 50)).thenReturn(List.of(
                NotificationResponse.builder()
                        .id(1L)
                        .recipientSubjectId(101L)
                        .recipientSubjectType("AGENT")
                        .build()
        ));

        var response = controller.listMine(50, 101L, SubjectType.AGENT).getData();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getRecipientSubjectType()).isEqualTo("AGENT");
        verify(notificationService).listNotifications(SubjectRef.agent(101L), 50);
    }

    @Test
    void markReadUsesExplicitAgentRecipientWhenLiableToPrincipalHuman() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        MarkNotificationReadRequest request = new MarkNotificationReadRequest();
        request.setNotificationId(88L);
        request.setRecipientSubjectId(101L);
        request.setRecipientSubjectType(SubjectType.AGENT);

        controller.markRead(request);

        verify(notificationService).markRead(SubjectRef.agent(101L), 88L);
    }

    @Test
    void unauthorizedAgentRecipientIsRejectedBeforeServiceCall() {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L))).thenReturn(LiabilityChain.agentToHuman(101L, 8L));

        assertThatThrownBy(() -> controller.listMine(20, 101L, SubjectType.AGENT))
                .isInstanceOf(BizException.class)
                .hasMessage("notification.access.denied");

        verifyNoInteractions(notificationService);
    }

    @Test
    void partialOrSystemRecipientParamsAreRejectedBeforePolicyCall() {
        MarkNotificationReadRequest missingRecipient = new MarkNotificationReadRequest();
        missingRecipient.setNotificationId(88L);

        assertThatThrownBy(() -> controller.markRead(missingRecipient))
                .isInstanceOf(BizException.class)
                .hasMessage("notification.recipient.type.invalid");

        assertThatThrownBy(() -> controller.listMine(20, 101L, null))
                .isInstanceOf(BizException.class)
                .hasMessage("notification.recipient.type.invalid");

        MarkNotificationReadRequest request = new MarkNotificationReadRequest();
        request.setNotificationId(88L);
        request.setRecipientSubjectId(0L);
        request.setRecipientSubjectType(SubjectType.SYSTEM);

        assertThatThrownBy(() -> controller.markRead(request))
                .isInstanceOf(BizException.class)
                .hasMessage("notification.recipient.type.invalid");

        verifyNoInteractions(liabilityPolicyApi, notificationService);
    }
}
