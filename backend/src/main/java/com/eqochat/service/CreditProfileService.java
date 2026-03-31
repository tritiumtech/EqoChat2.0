package com.eqochat.service;

import com.eqochat.dto.response.CreditProfileResponse;

public interface CreditProfileService {

    CreditProfileResponse getSubjectCreditProfile(Long subjectId, String subjectType);
}

