package com.eqochat.business.credit.mapper;

import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SubjectCreditProfileMapper {

    @Select("""
            SELECT
              score,
              rating,
              dispute_count AS disputeCount,
              projects_completed AS projectsCompleted,
              success_rate AS successRate
            FROM subject_credit_profile
            WHERE subject_id = #{subjectId}
              AND subject_type = #{subjectType}
              AND del_token = '0'
            LIMIT 1
            """)
    SubjectCreditProfileRow selectProfileBySubject(
            @Param("subjectId") Long subjectId,
            @Param("subjectType") String subjectType);

    @Data
    class SubjectCreditProfileRow {
        private Integer score;
        private String rating;
        private Integer disputeCount;
        private Integer projectsCompleted;
        private Integer successRate;
    }
}
