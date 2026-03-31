package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import com.eqochat.dto.response.CreditProfileResponse;
import com.eqochat.service.CreditProfileService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditProfileService creditProfileService;

    @GetMapping("/subject")
    public ApiResponse<CreditProfileResponse> getSubjectCreditProfile(
            @RequestParam @NotNull Long subjectId,
            @RequestParam String subjectType) {
        CreditProfileResponse profile = creditProfileService.getSubjectCreditProfile(subjectId, subjectType);
        return ApiResponse.success(profile);
    }
}

