package com.eqochat.business.contact.controller.friend;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.contact.api.service.FriendRequestService;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.common.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendRequestControllerSubjectInboxTest {

    @Mock
    FriendRequestService friendRequestService;

    FriendRequestController controller;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(9L);
        controller = new FriendRequestController(friendRequestService);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void receivedInboxRejectsMissingSubjectBeforeServiceCall() {
        assertThatThrownBy(() -> controller.listReceived(null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("friend_request.subject.invalid");

        verifyNoInteractions(friendRequestService);
    }

    @Test
    void sentInboxPassesExplicitAgentRequesterSubject() {
        when(friendRequestService.listSent(9L, SubjectRef.agent(101L))).thenReturn(List.of());

        controller.listSent(101L, SubjectType.AGENT);

        verify(friendRequestService).listSent(9L, SubjectRef.agent(101L));
    }

    @Test
    void partialOrSystemInboxSubjectParamsAreRejectedBeforeServiceCall() {
        assertThatThrownBy(() -> controller.listReceived(101L, null))
                .isInstanceOf(BizException.class)
                .hasMessage("friend_request.subject.invalid");

        assertThatThrownBy(() -> controller.listSent(0L, SubjectType.SYSTEM))
                .isInstanceOf(BizException.class)
                .hasMessage("friend_request.subject.invalid");

        verifyNoInteractions(friendRequestService);
    }
}
