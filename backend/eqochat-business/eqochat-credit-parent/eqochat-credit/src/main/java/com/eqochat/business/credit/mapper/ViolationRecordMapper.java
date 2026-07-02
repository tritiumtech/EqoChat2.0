package com.eqochat.business.credit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.credit.entity.ViolationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ViolationRecordMapper extends BaseMapper<ViolationRecord> {

    @Select("SELECT * FROM violation_record WHERE subject_id = #{subjectId} AND subject_type = #{subjectType} AND del_token = '0' ORDER BY create_time DESC")
    List<ViolationRecord> findBySubject(
            @Param("subjectId") Long subjectId,
            @Param("subjectType") String subjectType);

    @Select("SELECT * FROM violation_record WHERE status = #{status} AND del_token = '0' ORDER BY create_time DESC")
    List<ViolationRecord> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM violation_record WHERE reporter_id = #{reporterId} AND reporter_type = #{reporterType} AND del_token = '0' ORDER BY create_time DESC")
    List<ViolationRecord> findByReporter(
            @Param("reporterId") Long reporterId,
            @Param("reporterType") String reporterType);

    default List<ViolationRecord> findByReporter(SubjectRef reporter) {
        return findByReporter(reporter.id(), reporter.type().name());
    }

    @Select("SELECT * FROM violation_record WHERE reviewer_id = #{reviewerId} AND reviewer_type = #{reviewerType} AND del_token = '0' ORDER BY create_time DESC")
    List<ViolationRecord> findByReviewer(
            @Param("reviewerId") Long reviewerId,
            @Param("reviewerType") String reviewerType);

    default List<ViolationRecord> findByReviewer(SubjectRef reviewer) {
        return findByReviewer(reviewer.id(), reviewer.type().name());
    }

    @Select("SELECT COUNT(*) FROM violation_record WHERE subject_id = #{subjectId} AND subject_type = #{subjectType} AND status = 'CONFIRMED' AND del_token = '0'")
    long countConfirmedBySubject(
            @Param("subjectId") Long subjectId,
            @Param("subjectType") String subjectType);
}
