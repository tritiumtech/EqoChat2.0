package com.eqochat.business.project.controller.project;

import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.project.api.dto.request.CreateProjectPaymentRequest;
import com.eqochat.business.project.api.dto.request.CreateProjectRequest;
import com.eqochat.business.project.api.dto.request.CreateProjectTaskRequest;
import com.eqochat.business.project.api.dto.request.TransferProjectOwnershipRequest;
import com.eqochat.business.project.api.dto.request.UpdateProjectBidRequest;
import com.eqochat.business.project.api.service.ProjectService;
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
class ProjectControllerViewerSubjectTest {

    @Mock
    ProjectService projectService;

    ProjectController controller;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(9L);
        controller = new ProjectController(projectService);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void listProjectsRejectsMissingViewerSubjectBeforeServiceCall() {
        assertThatThrownBy(() -> controller.listMyProjects(null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("project.viewer.invalid");

        verifyNoInteractions(projectService);
    }

    @Test
    void listProjectsDispatchesExplicitViewerSubject() {
        when(projectService.listMyProjects(9L, 9L, SubjectType.HUMAN)).thenReturn(List.of());

        controller.listMyProjects(9L, SubjectType.HUMAN);

        verify(projectService).listMyProjects(9L, 9L, SubjectType.HUMAN);
    }

    @Test
    void detailRejectsSystemViewerBeforeServiceCall() {
        assertThatThrownBy(() -> controller.getProjectDetail(501L, 9L, SubjectType.SYSTEM))
                .isInstanceOf(BizException.class)
                .hasMessage("project.viewer.invalid");

        verifyNoInteractions(projectService);
    }

    @Test
    void sidebarRejectsPartialViewerSubjectBeforeServiceCall() {
        assertThatThrownBy(() -> controller.listSidebarTasks(501L, 9L, null))
                .isInstanceOf(BizException.class)
                .hasMessage("project.viewer.invalid");

        verifyNoInteractions(projectService);
    }

    @Test
    void createProjectDispatchesPrincipalAndExplicitOwnerSubject() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Agent owned");
        request.setBid(100L);
        request.setOwnerSubjectId(101L);
        request.setOwnerSubjectType(SubjectType.AGENT);

        controller.createProject(request);

        verify(projectService).createProject(9L, request);
    }

    @Test
    void bidUpdateDispatchesPrincipalAndExplicitActorSubject() {
        UpdateProjectBidRequest request = new UpdateProjectBidRequest();
        request.setNewBid(120L);
        request.setActorSubjectId(101L);
        request.setActorSubjectType(SubjectType.AGENT);

        controller.requestBidUpdate(501L, request);

        verify(projectService).requestBidUpdate(9L, 501L, request);
    }

    @Test
    void transferOwnershipDispatchesPrincipalAndExplicitActorSubject() {
        TransferProjectOwnershipRequest request = new TransferProjectOwnershipRequest();
        request.setNewOwnerSubjectId(102L);
        request.setNewOwnerSubjectType(SubjectType.AGENT);
        request.setActorSubjectId(101L);
        request.setActorSubjectType(SubjectType.AGENT);

        controller.transferOwnership(501L, request);

        verify(projectService).transferOwnership(9L, 501L, request);
    }

    @Test
    void createTaskDispatchesPrincipalAndExplicitActorSubject() {
        CreateProjectTaskRequest request = CreateProjectTaskRequest.builder()
                .title("Ship")
                .value(100L)
                .deadline("2026-08-01")
                .priority("medium")
                .assigneeSubjectId(102L)
                .assigneeSubjectType(SubjectType.AGENT)
                .actorSubjectId(101L)
                .actorSubjectType(SubjectType.AGENT)
                .build();

        controller.createTask(501L, request);

        verify(projectService).createTask(9L, 501L, request);
    }

    @Test
    void createPaymentDispatchesPrincipalAndExplicitActorSubject() {
        CreateProjectPaymentRequest request = CreateProjectPaymentRequest.builder()
                .amount(100L)
                .recipientSubjectId(102L)
                .recipientSubjectType(SubjectType.AGENT)
                .actorSubjectId(101L)
                .actorSubjectType(SubjectType.AGENT)
                .build();

        controller.createPayment(501L, request);

        verify(projectService).createPayment(9L, 501L, request);
    }
}
