package com.eqochat.business.credit.service.impl;

import com.eqochat.business.credit.entity.CreditRecord;
import com.eqochat.business.credit.mapper.CreditRecordMapper;
import com.eqochat.framework.common.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditEarningsServiceImplTest {

    @Mock
    CreditRecordMapper creditRecordMapper;

    @Test
    void positiveChangeTotalSumsOnlyPositiveCanonicalSubjectRecords() {
        when(creditRecordMapper.findBySubject(101L, "AGENT"))
                .thenReturn(List.of(credit(25), credit(-5), credit(null), credit(7)));

        long total = new CreditEarningsServiceImpl(creditRecordMapper)
                .positiveChangeTotal(101L, "agent");

        assertThat(total).isEqualTo(32L);
        verify(creditRecordMapper).findBySubject(101L, "AGENT");
    }

    @Test
    void positiveChangeTotalRejectsNonCanonicalSubjectType() {
        CreditEarningsServiceImpl service = new CreditEarningsServiceImpl(creditRecordMapper);

        assertThatThrownBy(() -> service.positiveChangeTotal(101L, "USER"))
                .isInstanceOf(BizException.class);
    }

    private static CreditRecord credit(Integer changeAmount) {
        return CreditRecord.builder()
                .changeAmount(changeAmount)
                .build();
    }
}
