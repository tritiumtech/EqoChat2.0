package com.eqochat.business.credit.service.impl;

import com.eqochat.business.credit.mapper.CreditRecordMapper;
import com.eqochat.business.credit.mapper.SubjectCreditScoreMapper;
import com.eqochat.business.credit.mapper.ViolationRecordMapper;
import com.eqochat.framework.common.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditProfileServiceImplActorContractTest {

    @Mock
    CreditRecordMapper creditRecordMapper;

    @Mock
    ViolationRecordMapper violationRecordMapper;

    @Mock
    SubjectCreditScoreMapper subjectCreditScoreMapper;

    @Test
    void rejectsLegacyUserSubjectTypeWithoutMapperFallback() {
        CreditProfileServiceImpl service = service();

        assertThatThrownBy(() -> service.getSubjectCreditProfile(2L, "USER"))
                .isInstanceOf(BizException.class)
                .hasMessage("error.invalid.subject_type");

        verifyNoInteractions(creditRecordMapper, violationRecordMapper, subjectCreditScoreMapper);
    }

    @Test
    void humanSubjectTypeIsQueriedCanonically() {
        when(creditRecordMapper.findBySubject(2L, "HUMAN")).thenReturn(List.of());
        when(violationRecordMapper.findBySubject(2L, "HUMAN")).thenReturn(List.of());
        when(subjectCreditScoreMapper.selectUserCreditScoreById(2L)).thenReturn(720);

        CreditProfileServiceImpl service = service();

        assertThat(service.getSubjectCreditProfile(2L, "HUMAN").getCreditScore()).isEqualTo(720);

        verify(creditRecordMapper).findBySubject(2L, "HUMAN");
        verify(violationRecordMapper).findBySubject(2L, "HUMAN");
        verify(subjectCreditScoreMapper).selectUserCreditScoreById(2L);
    }

    private CreditProfileServiceImpl service() {
        return new CreditProfileServiceImpl(creditRecordMapper, violationRecordMapper, subjectCreditScoreMapper);
    }
}
