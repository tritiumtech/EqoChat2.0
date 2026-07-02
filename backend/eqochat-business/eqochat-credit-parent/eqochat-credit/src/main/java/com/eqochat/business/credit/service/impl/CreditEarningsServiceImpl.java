package com.eqochat.business.credit.service.impl;

import com.eqochat.business.credit.api.service.CreditEarningsService;
import com.eqochat.business.credit.entity.CreditRecord;
import com.eqochat.business.credit.mapper.CreditRecordMapper;
import com.eqochat.framework.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditEarningsServiceImpl implements CreditEarningsService {

    private final CreditRecordMapper creditRecordMapper;

    @Override
    public long positiveChangeTotal(Long subjectId, String subjectType) {
        if (subjectId == null) {
            return 0;
        }
        String canonicalType = parseSubjectType(subjectType).name();
        List<CreditRecord> records = creditRecordMapper.findBySubject(subjectId, canonicalType);
        if (records == null || records.isEmpty()) {
            return 0;
        }
        return records.stream()
                .filter(record -> record != null && record.getChangeAmount() != null && record.getChangeAmount() > 0)
                .mapToLong(record -> record.getChangeAmount().longValue())
                .sum();
    }

    private static CreditRecord.SubjectType parseSubjectType(String subjectType) {
        try {
            return CreditRecord.SubjectType.valueOf(subjectType == null ? "" : subjectType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw BizException.of("error.invalid.subject_type");
        }
    }
}
