package com.eqochat.business.project.api.service;

import com.eqochat.business.project.api.dto.request.CreateProjectRequest;
import com.eqochat.business.project.api.dto.request.CreateProjectPaymentRequest;
import com.eqochat.business.project.api.dto.request.CreateProjectTaskRequest;
import com.eqochat.business.project.api.dto.request.TransferProjectOwnershipRequest;
import com.eqochat.business.project.api.dto.request.UpdateProjectBidRequest;
import com.eqochat.business.project.api.dto.response.*;

import java.util.List;

public interface ProjectService {

    List<ProjectSummaryResponse> listMyProjects(Long viewerId);

    ProjectDetailResponse createProject(Long viewerId, CreateProjectRequest request);

    ProjectDetailResponse getProjectDetail(Long viewerId, Long projectId);

    void requestBidUpdate(Long viewerId, Long projectId, UpdateProjectBidRequest request);

    void transferOwnership(Long viewerId, Long projectId, TransferProjectOwnershipRequest request);

    ProjectShareLinkResponse shareLink(Long viewerId, Long projectId);

    List<ProjectTaskResponse> listSidebarTasks(Long viewerId, Long projectId);

    List<ProjectPaymentResponse> listSidebarPayments(Long viewerId, Long projectId);

    List<ProjectFileResponse> listSidebarFiles(Long viewerId, Long projectId);

    ProjectTaskResponse createTask(Long viewerId, Long projectId, CreateProjectTaskRequest request);

    ProjectPaymentResponse createPayment(Long viewerId, Long projectId, CreateProjectPaymentRequest request);
}
