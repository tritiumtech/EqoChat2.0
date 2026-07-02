package com.eqochat.business.credit.service.impl;

import com.eqochat.business.credit.entity.CreditRecord;
import com.eqochat.business.credit.entity.ViolationRecord;
import com.eqochat.business.credit.mapper.CreditRecordMapper;
import com.eqochat.business.credit.mapper.SubjectCreditProfileMapper;
import com.eqochat.business.credit.mapper.ViolationRecordMapper;
import com.eqochat.framework.common.BizException;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
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
    SubjectCreditProfileMapper subjectCreditProfileMapper;

    @Test
    void rejectsLegacyUserSubjectTypeWithoutMapperFallback() {
        CreditProfileServiceImpl service = service();

        assertThatThrownBy(() -> service.getSubjectCreditProfile(2L, "USER"))
                .isInstanceOf(BizException.class)
                .hasMessage("error.invalid.subject_type");

        verifyNoInteractions(creditRecordMapper, violationRecordMapper, subjectCreditProfileMapper);
    }

    @Test
    void subjectCreditProfileIsCanonicalScoreSource() {
        SubjectCreditProfileMapper.SubjectCreditProfileRow profile = profile(720, "GOOD", 1, 7, 86);
        when(subjectCreditProfileMapper.selectProfileBySubject(2L, "HUMAN")).thenReturn(profile);
        when(creditRecordMapper.findBySubject(2L, "HUMAN")).thenReturn(List.of(
                CreditRecord.builder().id(1L).currentScore(10).changeAmount(10).relatedType("PROJECT").build()
        ));
        when(violationRecordMapper.findBySubject(2L, "HUMAN")).thenReturn(List.of());

        var response = service().getSubjectCreditProfile(2L, "HUMAN");

        assertThat(response.getCreditScore()).isEqualTo(720);
        assertThat(response.getProjectsCompleted()).isEqualTo(7);
        assertThat(response.getDisputeCount()).isEqualTo(1);
        assertThat(response.getSuccessRate()).isEqualTo(86);
        verify(subjectCreditProfileMapper).selectProfileBySubject(2L, "HUMAN");
    }

    @Test
    void latestCreditRecordScoreIsFallbackWhenProfileRowIsMissing() {
        when(subjectCreditProfileMapper.selectProfileBySubject(101L, "AGENT")).thenReturn(null);
        when(creditRecordMapper.findBySubject(101L, "AGENT")).thenReturn(List.of(CreditRecord.builder()
                .currentScore(50)
                .changeAmount(20)
                .relatedType("PROJECT")
                .build()));
        when(violationRecordMapper.findBySubject(101L, "AGENT")).thenReturn(List.of());

        var response = service().getSubjectCreditProfile(101L, "AGENT");

        assertThat(response.getCreditScore()).isEqualTo(575);
        assertThat(response.getProjectsCompleted()).isEqualTo(1);
        assertThat(response.getSuccessRate()).isEqualTo(100);
    }

    @Test
    void violationRecordsRemainAuditDisputeSource() {
        ViolationRecord confirmed = ViolationRecord.builder()
                .id(11L)
                .violationType("Scope dispute")
                .description("missed milestone")
                .status(ViolationRecord.ViolationStatus.CONFIRMED)
                .reporterId(2L)
                .reviewedAt(LocalDateTime.of(2026, 6, 20, 12, 0))
                .build();
        when(subjectCreditProfileMapper.selectProfileBySubject(101L, "AGENT")).thenReturn(null);
        when(creditRecordMapper.findBySubject(101L, "AGENT")).thenReturn(List.of(CreditRecord.builder()
                .currentScore(680)
                .changeAmount(20)
                .relatedType("PROJECT")
                .build()));
        when(violationRecordMapper.findBySubject(101L, "AGENT")).thenReturn(List.of(confirmed));

        var response = service().getSubjectCreditProfile(101L, "AGENT");

        assertThat(response.getDisputeCount()).isEqualTo(1);
        assertThat(response.getSuccessRate()).isEqualTo(0);
        assertThat(response.getDisputes()).hasSize(1);
        assertThat(response.getDisputes().get(0).getVerdict()).isEqualTo("verified");
    }

    @Test
    void missingProfileAndRecordsDefaultsToPrdMinimum() {
        when(subjectCreditProfileMapper.selectProfileBySubject(2L, "HUMAN")).thenReturn(null);
        when(creditRecordMapper.findBySubject(2L, "HUMAN")).thenReturn(List.of());
        when(violationRecordMapper.findBySubject(2L, "HUMAN")).thenReturn(List.of());

        assertThat(service().getSubjectCreditProfile(2L, "HUMAN").getCreditScore()).isEqualTo(300);
    }

    @Test
    void subjectCreditProfileMapperDoesNotReadLegacyProfileTables() throws Exception {
        String sql = selectSql(SubjectCreditProfileMapper.class, "selectProfileBySubject");

        assertThat(sql).contains("FROM subject_credit_profile");
        assertThat(sql).doesNotContain("user_profile");
        assertThat(sql).doesNotContain("agent_profile");
    }

    @Test
    void creditAuditActorQueriesAreSubjectAware() throws Exception {
        String operatorSql = selectSql(CreditRecordMapper.class, "findByOperator");
        String reporterSql = selectSql(ViolationRecordMapper.class, "findByReporter");
        String reviewerSql = selectSql(ViolationRecordMapper.class, "findByReviewer");

        assertThat(operatorSql).contains("operator_id = #{operatorId}");
        assertThat(operatorSql).contains("operator_type = #{operatorType}");
        assertThat(reporterSql).contains("reporter_id = #{reporterId}");
        assertThat(reporterSql).contains("reporter_type = #{reporterType}");
        assertThat(reviewerSql).contains("reviewer_id = #{reviewerId}");
        assertThat(reviewerSql).contains("reviewer_type = #{reviewerType}");
    }

    private CreditProfileServiceImpl service() {
        return new CreditProfileServiceImpl(creditRecordMapper, violationRecordMapper, subjectCreditProfileMapper);
    }

    private static SubjectCreditProfileMapper.SubjectCreditProfileRow profile(
            Integer score,
            String rating,
            Integer disputeCount,
            Integer projectsCompleted,
            Integer successRate
    ) {
        SubjectCreditProfileMapper.SubjectCreditProfileRow row =
                new SubjectCreditProfileMapper.SubjectCreditProfileRow();
        row.setScore(score);
        row.setRating(rating);
        row.setDisputeCount(disputeCount);
        row.setProjectsCompleted(projectsCompleted);
        row.setSuccessRate(successRate);
        return row;
    }

    private static String selectSql(Class<?> mapperType, String methodName) throws Exception {
        for (Method method : mapperType.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                Select select = method.getAnnotation(Select.class);
                if (select != null) {
                    return String.join("\n", select.value());
                }
            }
        }
        throw new IllegalArgumentException("method not found: " + methodName);
    }
}
