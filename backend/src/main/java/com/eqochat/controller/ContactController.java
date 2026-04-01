package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import com.eqochat.common.UserContext;
import com.eqochat.dto.request.UpdateContactTagsRequest;
import com.eqochat.dto.response.ContactDetailResponse;
import com.eqochat.dto.response.ContactResponse;
import com.eqochat.service.ContactService;
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
                                                          @RequestParam(required = false) String status) {
        List<ContactResponse> list = contactService.listContacts(UserContext.requireCurrentUser());
        if (q != null && !q.isBlank()) {
            String keyword = q.trim().toLowerCase();
            list = list.stream()
                    .filter(item -> {
                        String name = item.getNickname() != null ? item.getNickname() : "";
                        return name.toLowerCase().contains(keyword) || String.valueOf(item.getId()).contains(keyword);
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

    @GetMapping("/{contactId}")
    public ApiResponse<ContactDetailResponse> getContactDetail(@PathVariable Long contactId) {
        return ApiResponse.success(contactService.getContactDetail(UserContext.requireCurrentUser(), contactId));
    }

    @PutMapping("/{contactId}/tags")
    public ApiResponse<List<String>> updateContactTags(@PathVariable Long contactId,
                                                       @RequestBody(required = false) @Valid UpdateContactTagsRequest request) {
        List<String> tags = request != null ? request.getTags() : null;
        return ApiResponse.success(contactService.updateContactTags(UserContext.requireCurrentUser(), contactId, tags));
    }
}
