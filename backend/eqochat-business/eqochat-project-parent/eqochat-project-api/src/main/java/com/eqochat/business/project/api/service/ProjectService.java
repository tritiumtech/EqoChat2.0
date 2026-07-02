package com.eqochat.business.project.api.service;

import com.eqochat.business.project.api.dto.request.CreateProjectRequest;
import com.eqochat.business.project.api.dto.request.CreateProjectPaymentRequest;
import com.eqochat.business.project.api.dto.request.CreateProjectTaskRequest;
import com.eqochat.business.project.api.dto.request.TransferProjectOwnershipRequest;
import com.eqochat.business.project.api.dto.request.UpdateProjectBidRequest;
import com.eqochat.business.project.api.dto.response.*;
import com.eqochat.business.actor.api.model.SubjectType;

import java.util.List;

public interface ProjectService {

    List<ProjectSummaryResponse> listMyProjects(Long principalHumanId, Long viewerSubjectId, SubjectType viewerSubjectType);

    ProjectDetailResponse createProject(Long principalHumanId, CreateProjectRequest request);

    ProjectDetailResponse getProjectDetail(Long principalHumanId, Long projectId, Long viewerSubjectId, SubjectType viewerSubjectType);

    void requestBidUpdate(Long principalHumanId, Long projectId, UpdateProjectBidRequest request);

    void transferOwnership(Long principalHumanId, Long projectId, TransferProjectOwnershipRequest request);

    ProjectShareLinkResponse shareLink(Long principalHumanId, Long projectId, Long viewerSubjectId, SubjectType viewerSubjectType);

    List<ProjectTaskResponse> listSidebarTasks(Long principalHumanId, Long projectId, Long viewerSubjectId, SubjectType viewerSubjectType);

    List<ProjectPaymentResponse> listSidebarPayments(Long principalHumanId, Long projectId, Long viewerSubjectId, SubjectType viewerSubjectType);

    List<ProjectFileResponse> listSidebarFiles(Long principalHumanId, Long projectId, Long viewerSubjectId, SubjectType viewerSubjectType);

    ProjectTaskResponse createTask(Long principalHumanId, Long projectId, CreateProjectTaskRequest request);

    ProjectPaymentResponse createPayment(Long principalHumanId, Long projectId, CreateProjectPaymentRequest request);
}
