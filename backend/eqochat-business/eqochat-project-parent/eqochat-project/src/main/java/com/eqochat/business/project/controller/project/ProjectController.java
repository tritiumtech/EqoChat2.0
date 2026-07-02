package com.eqochat.business.project.controller.project;

import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.common.UserContext;
import com.eqochat.business.actor.api.model.SubjectType;
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
    public ApiResponse<List<ProjectSummaryResponse>> listMyProjects(
            @RequestParam(required = false) Long viewerSubjectId,
            @RequestParam(required = false) SubjectType viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        requireExplicitViewerSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(projectService.listMyProjects(principalHumanId, viewerSubjectId, viewerSubjectType));
    }

    @PostMapping
    public ApiResponse<ProjectDetailResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        Long principalHumanId = UserContext.requireCurrentUser();
        return ApiResponse.success(projectService.createProject(principalHumanId, request));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDetailResponse> getProjectDetail(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long viewerSubjectId,
            @RequestParam(required = false) SubjectType viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        requireExplicitViewerSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(projectService.getProjectDetail(principalHumanId, projectId, viewerSubjectId, viewerSubjectType));
    }

    @PostMapping("/{projectId}/bid-update")
    public ApiResponse<Void> requestBidUpdate(@PathVariable Long projectId,
                                                 @Valid @RequestBody UpdateProjectBidRequest request) {
        Long principalHumanId = UserContext.requireCurrentUser();
        projectService.requestBidUpdate(principalHumanId, projectId, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/{projectId}/transfer")
    public ApiResponse<Void> transferOwnership(@PathVariable Long projectId,
                                                 @Valid @RequestBody TransferProjectOwnershipRequest request) {
        Long principalHumanId = UserContext.requireCurrentUser();
        projectService.transferOwnership(principalHumanId, projectId, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/{projectId}/share-link")
    public ApiResponse<ProjectShareLinkResponse> shareLink(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long viewerSubjectId,
            @RequestParam(required = false) SubjectType viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        requireExplicitViewerSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(projectService.shareLink(principalHumanId, projectId, viewerSubjectId, viewerSubjectType));
    }

    @GetMapping("/{projectId}/sidebar/tasks")
    public ApiResponse<List<ProjectTaskResponse>> listSidebarTasks(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long viewerSubjectId,
            @RequestParam(required = false) SubjectType viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        requireExplicitViewerSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(projectService.listSidebarTasks(principalHumanId, projectId, viewerSubjectId, viewerSubjectType));
    }

    @GetMapping("/{projectId}/sidebar/payments")
    public ApiResponse<List<ProjectPaymentResponse>> listSidebarPayments(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long viewerSubjectId,
            @RequestParam(required = false) SubjectType viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        requireExplicitViewerSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(projectService.listSidebarPayments(principalHumanId, projectId, viewerSubjectId, viewerSubjectType));
    }

    @GetMapping("/{projectId}/sidebar/files")
    public ApiResponse<List<ProjectFileResponse>> listSidebarFiles(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long viewerSubjectId,
            @RequestParam(required = false) SubjectType viewerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        requireExplicitViewerSubject(viewerSubjectId, viewerSubjectType);
        return ApiResponse.success(projectService.listSidebarFiles(principalHumanId, projectId, viewerSubjectId, viewerSubjectType));
    }

    @PostMapping("/{projectId}/tasks")
    public ApiResponse<ProjectTaskResponse> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateProjectTaskRequest request) {
        Long principalHumanId = UserContext.requireCurrentUser();
        return ApiResponse.success(projectService.createTask(principalHumanId, projectId, request));
    }

    @PostMapping("/{projectId}/payments")
    public ApiResponse<ProjectPaymentResponse> createPayment(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateProjectPaymentRequest request) {
        Long principalHumanId = UserContext.requireCurrentUser();
        return ApiResponse.success(projectService.createPayment(principalHumanId, projectId, request));
    }

    private static void requireExplicitViewerSubject(Long viewerSubjectId, SubjectType viewerSubjectType) {
        if (viewerSubjectId == null || viewerSubjectType == null || viewerSubjectType == SubjectType.SYSTEM) {
            throw BizException.of("project.viewer.invalid");
        }
    }
}
