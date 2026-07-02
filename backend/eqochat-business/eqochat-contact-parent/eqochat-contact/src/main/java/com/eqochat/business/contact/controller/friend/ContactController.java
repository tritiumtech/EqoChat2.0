package com.eqochat.business.contact.controller.friend;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.common.UserContext;
import com.eqochat.business.contact.api.dto.request.UpdateContactTagsRequest;
import com.eqochat.business.contact.api.dto.response.ContactDetailResponse;
import com.eqochat.business.contact.api.dto.response.ContactResponse;
import com.eqochat.business.contact.api.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 联系人接口，好友需通过好友申请流程添加
 */
@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public ApiResponse<List<ContactResponse>> listContacts(@RequestParam(required = false) String q,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) Long ownerSubjectId,
                                                          @RequestParam(required = false) SubjectType ownerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef owner = resolveOwner(ownerSubjectId, ownerSubjectType);
        List<ContactResponse> list = contactService.listContacts(principalHumanId, owner);
        if (q != null && !q.isBlank()) {
            String keyword = q.trim().toLowerCase();
            list = list.stream()
                    .filter(item -> {
                        String name = item.getNickname() != null ? item.getNickname() : "";
                        return name.toLowerCase().contains(keyword)
                                || String.valueOf(item.getTargetSubjectId()).contains(keyword);
                    })
                    .toList();
        }
        if (status != null && !status.isBlank()) {
            String want = status.trim().toUpperCase();
            list = list.stream()
                    .filter(item -> item.getStatus() != null && item.getStatus().toUpperCase().contains(want))
                    .toList();
        }
        return ApiResponse.success(list);
    }

    @GetMapping("/{targetType}/{targetId}")
    public ApiResponse<ContactDetailResponse> getContactDetail(@PathVariable SubjectType targetType,
                                                               @PathVariable Long targetId,
                                                               @RequestParam(required = false) Long ownerSubjectId,
                                                               @RequestParam(required = false) SubjectType ownerSubjectType) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef owner = resolveOwner(ownerSubjectId, ownerSubjectType);
        return ApiResponse.success(contactService.getContactDetail(
                principalHumanId,
                owner,
                new SubjectRef(targetId, targetType)));
    }

    @PutMapping("/{targetType}/{targetId}/tags")
    public ApiResponse<List<String>> updateContactTags(@PathVariable SubjectType targetType,
                                                       @PathVariable Long targetId,
                                                       @RequestParam(required = false) Long ownerSubjectId,
                                                       @RequestParam(required = false) SubjectType ownerSubjectType,
                                                       @RequestBody(required = false) @Valid UpdateContactTagsRequest request) {
        Long principalHumanId = UserContext.requireCurrentUser();
        SubjectRef owner = resolveOwner(ownerSubjectId, ownerSubjectType);
        List<String> tags = request != null ? request.getTags() : null;
        return ApiResponse.success(contactService.updateContactTags(
                principalHumanId,
                owner,
                new SubjectRef(targetId, targetType),
                tags));
    }

    private static SubjectRef resolveOwner(Long ownerSubjectId, SubjectType ownerSubjectType) {
        if (ownerSubjectId == null || ownerSubjectType == null) {
            throw BizException.of("contact.subject.invalid");
        }
        if (ownerSubjectType == SubjectType.SYSTEM) {
            throw BizException.of("contact.subject.invalid");
        }
        return new SubjectRef(ownerSubjectId, ownerSubjectType);
    }
}
