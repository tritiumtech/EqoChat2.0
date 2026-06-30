package com.eqochat.business.project.controller.project;

import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.common.UserContext;
import com.eqochat.business.project.api.dto.request.CreateProjectRequest;
import com.eqochat.business.project.api.dto.request.CreateProjectPaymentRequest;
import com.eqochat.business.project.api.dto.request.CreateProjectTaskRequest;
import com.eqochat.business.project.api.dto.request.TransferProjectOwnershipRequest;
import com.eqochat.business.project.api.dto.request.UpdateProjectBidRequest;
import com.eqochat.business.project.api.dto.response.*;
import com.eqochat.business.project.api.service.ProjectService;
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

    @PostMapping("/{projectId}/tasks")
    public ApiResponse<ProjectTaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateProjectTaskRequest request) {
        Long viewerId = UserContext.requireCurrentUser();
        return ApiResponse.success(projectService.createTask(viewerId, projectId, request));
    }

    @PostMapping("/{projectId}/payments")
    public ApiResponse<ProjectPaymentResponse> createPayment(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateProjectPaymentRequest request) {
        Long viewerId = UserContext.requireCurrentUser();
        return ApiResponse.success(projectService.createPayment(viewerId, projectId, request));
    }
}
