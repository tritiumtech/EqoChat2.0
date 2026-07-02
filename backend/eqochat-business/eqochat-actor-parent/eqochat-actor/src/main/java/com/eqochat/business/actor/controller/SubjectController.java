package com.eqochat.business.actor.controller;

import com.eqochat.business.actor.api.dto.response.SubjectPublicProfileResponse;
import com.eqochat.business.actor.api.dto.response.SubjectSearchResponse;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.SubjectProfileApi;
import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectProfileApi subjectProfileService;

    @GetMapping("/search")
    public ApiResponse<List<SubjectSearchResponse>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Long viewerSubjectId,
            @RequestParam(required = false) SubjectType viewerSubjectType
    ) {
        requireExplicitViewerSubject(viewerSubjectId, viewerSubjectType);
        Long principalHumanId = UserContext.requireCurrentUser();
        return ApiResponse.success(subjectProfileService.search(
                principalHumanId,
                keyword,
                viewerSubjectId,
                viewerSubjectType));
    }

    @GetMapping("/{subjectType}/{subjectId}/public")
    public ApiResponse<SubjectPublicProfileResponse> publicProfile(
            @PathVariable String subjectType,
            @PathVariable Long subjectId,
            @RequestParam(required = false) Long viewerSubjectId,
            @RequestParam(required = false) SubjectType viewerSubjectType
    ) {
        requireExplicitViewerSubject(viewerSubjectId, viewerSubjectType);
        Long principalHumanId = UserContext.requireCurrentUser();
        return ApiResponse.success(subjectProfileService.getPublicProfile(
                principalHumanId,
                parseSubjectType(subjectType),
                subjectId,
                viewerSubjectId,
                viewerSubjectType));
    }

    private static void requireExplicitViewerSubject(Long viewerSubjectId, SubjectType viewerSubjectType) {
        if (viewerSubjectId == null || viewerSubjectType == null || viewerSubjectType == SubjectType.SYSTEM) {
            throw BizException.of("subject.viewer.invalid");
        }
    }

    private SubjectType parseSubjectType(String raw) {
        try {
            SubjectType type = SubjectType.from(raw);
            if (type == SubjectType.SYSTEM) {
                throw new IllegalArgumentException("system public profile is not supported");
            }
            return type;
        } catch (IllegalArgumentException | NullPointerException e) {
            throw BizException.of("subject.type.invalid");
        }
    }
}
