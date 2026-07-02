package com.eqochat.business.chat.websocket;

import com.eqochat.framework.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketSessionManagerActorContractTest {

    @Test
    void replacedPrincipalSessionOldCloseDoesNotRemoveNewSession() {
        WebSocketSession oldSession = mock(WebSocketSession.class);
        WebSocketSession newSession = mock(WebSocketSession.class);
        when(oldSession.getId()).thenReturn("old-session");
        when(newSession.getId()).thenReturn("new-session");
        WebSocketSessionManager manager = new WebSocketSessionManager();

        manager.registerPrincipalHumanSession("2", oldSession);
        manager.registerSubjectSession("2", "101", "AGENT", oldSession);
        manager.registerPrincipalHumanSession("2", newSession);
        boolean removedCurrent = manager.unregisterPrincipalHumanSession("2", oldSession);

        assertThat(removedCurrent).isFalse();
        assertThat(manager.getPrincipalHumanSession("2")).isSameAs(newSession);
        assertThat(manager.getSubjectKeysBySessionId("old-session")).isEmpty();
        assertThat(manager.getSubjectKeysBySessionId("new-session")).isEmpty();
        assertThat(manager.getSubjectSessions("2", "HUMAN")).doesNotContain(oldSession, newSession);
        assertThat(manager.getSubjectSessions("101", "AGENT")).doesNotContain(oldSession);
    }

    @Test
    void subjectOnlineUsesSubjectSessionIndex() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("agent-session");
        when(session.isOpen()).thenReturn(true);
        WebSocketSessionManager manager = new WebSocketSessionManager();

        manager.registerSubjectSession("2", "101", "AGENT", session);

        assertThat(manager.isSubjectOnline("101", "AGENT")).isTrue();
        assertThat(manager.isSubjectOnline("2", "HUMAN")).isFalse();

        manager.unregisterActiveSubjectSession(session);

        assertThat(manager.isSubjectOnline("101", "AGENT")).isFalse();
    }
}
