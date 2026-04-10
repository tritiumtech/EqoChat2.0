package com.eqochat.business.credit.api.service;

import com.eqochat.business.credit.api.dto.response.CreditProfileResponse;

public interface CreditProfileService {

    CreditProfileResponse getSubjectCreditProfile(Long subjectId, String subjectType);
}

