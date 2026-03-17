package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import com.eqochat.common.BizException;
import com.eqochat.dto.response.ContactResponse;
import com.eqochat.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ApiResponse<List<ContactResponse>> listContacts() {
        Long userId = resolveUserId();
        return ApiResponse.success(contactService.listContacts(userId));
    }
    
    private Long resolveUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw BizException.of(401, "auth.token.invalid");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        throw BizException.of(401, "auth.token.invalid");
    }
}
