package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.ViolationRecord;
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
    
    @Select("SELECT * FROM violation_record WHERE reporter_id = #{reporterId} AND del_token = '0' ORDER BY create_time DESC")
    List<ViolationRecord> findByReporterId(@Param("reporterId") Long reporterId);
    
    @Select("SELECT * FROM violation_record WHERE reviewer_id = #{reviewerId} AND del_token = '0' ORDER BY create_time DESC")
    List<ViolationRecord> findByReviewerId(@Param("reviewerId") Long reviewerId);
    
    @Select("SELECT COUNT(*) FROM violation_record WHERE subject_id = #{subjectId} AND subject_type = #{subjectType} AND status = 'CONFIRMED' AND del_token = '0'")
    long countConfirmedBySubject(
            @Param("subjectId") Long subjectId, 
            @Param("subjectType") String subjectType);
}
