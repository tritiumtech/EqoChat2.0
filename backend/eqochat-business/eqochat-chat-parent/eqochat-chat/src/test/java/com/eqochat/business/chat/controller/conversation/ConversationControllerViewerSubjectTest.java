package com.eqochat.business.chat.controller.conversation;

import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.chat.api.service.ConversationService;
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
class ConversationControllerViewerSubjectTest {

    @Mock
    ConversationService conversationService;

    ConversationController controller;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(2L);
        controller = new ConversationController(conversationService);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void listRejectsMissingViewerSubjectBeforeServiceCall() {
        assertThatThrownBy(() -> controller.listConversations(null, null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("conv.viewer.invalid");

        verifyNoInteractions(conversationService);
    }

    @Test
    void listDispatchesExplicitViewerSubject() {
        when(conversationService.listConversations(2L, 2L, SubjectType.HUMAN)).thenReturn(List.of());

        controller.listConversations(null, 2L, SubjectType.HUMAN);

        verify(conversationService).listConversations(2L, 2L, SubjectType.HUMAN);
    }

    @Test
    void detailRejectsSystemViewerBeforeServiceCall() {
        assertThatThrownBy(() -> controller.getConversation(10002L, 2L, SubjectType.SYSTEM))
                .isInstanceOf(BizException.class)
                .hasMessage("conv.viewer.invalid");

        verifyNoInteractions(conversationService);
    }

    @Test
    void messagesRejectPartialViewerSubjectBeforeServiceCall() {
        assertThatThrownBy(() -> controller.getMessages(10002L, null, 20, 2L, null))
                .isInstanceOf(BizException.class)
                .hasMessage("conv.viewer.invalid");

        verifyNoInteractions(conversationService);
    }
}
