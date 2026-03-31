package com.eqochat.project;

import com.eqochat.dto.request.CreateProjectRequest;
import com.eqochat.dto.request.TransferProjectOwnershipRequest;
import com.eqochat.dto.request.UpdateProjectBidRequest;
import com.eqochat.dto.response.*;

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
}

