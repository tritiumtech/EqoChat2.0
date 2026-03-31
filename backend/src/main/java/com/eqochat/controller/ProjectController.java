package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import com.eqochat.common.UserContext;
import com.eqochat.dto.request.CreateProjectRequest;
import com.eqochat.dto.request.TransferProjectOwnershipRequest;
import com.eqochat.dto.request.UpdateProjectBidRequest;
import com.eqochat.dto.response.*;
import com.eqochat.project.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ApiResponse<List<ProjectSummaryResponse>> listMyProjects() {
        Long viewerId = UserContext.requireCurrentUser();
        return ApiResponse.success(projectService.listMyProjects(viewerId));
    }

    @PostMapping
    public ApiResponse<ProjectDetailResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        Long viewerId = UserContext.requireCurrentUser();
        return ApiResponse.success(projectService.createProject(viewerId, request));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDetailResponse> getProjectDetail(@PathVariable Long projectId) {
        Long viewerId = UserContext.requireCurrentUser();
        return ApiResponse.success(projectService.getProjectDetail(viewerId, projectId));
    }

    @PostMapping("/{projectId}/bid-update")
    public ApiResponse<Void> requestBidUpdate(@PathVariable Long projectId,
                                                 @Valid @RequestBody UpdateProjectBidRequest request) {
        Long viewerId = UserContext.requireCurrentUser();
        projectService.requestBidUpdate(viewerId, projectId, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/{projectId}/transfer")
    public ApiResponse<Void> transferOwnership(@PathVariable Long projectId,
                                                 @Valid @RequestBody TransferProjectOwnershipRequest request) {
        Long viewerId = UserContext.requireCurrentUser();
        projectService.transferOwnership(viewerId, projectId, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/{projectId}/share-link")
    public ApiResponse<ProjectShareLinkResponse> shareLink(@PathVariable Long projectId) {
        Long viewerId = UserContext.requireCurrentUser();
        return ApiResponse.success(projectService.shareLink(viewerId, projectId));
    }

    @GetMapping("/{projectId}/sidebar/tasks")
    public ApiResponse<List<ProjectTaskResponse>> listSidebarTasks(@PathVariable Long projectId) {
        Long viewerId = UserContext.requireCurrentUser();
        return ApiResponse.success(projectService.listSidebarTasks(viewerId, projectId));
    }

    @GetMapping("/{projectId}/sidebar/payments")
    public ApiResponse<List<ProjectPaymentResponse>> listSidebarPayments(@PathVariable Long projectId) {
        Long viewerId = UserContext.requireCurrentUser();
        return ApiResponse.success(projectService.listSidebarPayments(viewerId, projectId));
    }

    @GetMapping("/{projectId}/sidebar/files")
    public ApiResponse<List<ProjectFileResponse>> listSidebarFiles(@PathVariable Long projectId) {
        Long viewerId = UserContext.requireCurrentUser();
        return ApiResponse.success(projectService.listSidebarFiles(viewerId, projectId));
    }
}

