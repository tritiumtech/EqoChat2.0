package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.CreditRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CreditRecordMapper extends BaseMapper<CreditRecord> {
    
    @Select("SELECT * FROM credit_record WHERE subject_id = #{subjectId} AND subject_type = #{subjectType} AND del_token = '0' ORDER BY create_time DESC")
    List<CreditRecord> findBySubject(
            @Param("subjectId") Long subjectId, 
            @Param("subjectType") String subjectType);
    
    @Select("SELECT * FROM credit_record WHERE subject_id = #{subjectId} AND subject_type = #{subjectType} AND del_token = '0' ORDER BY create_time DESC LIMIT #{limit}")
    List<CreditRecord> findBySubjectWithLimit(
            @Param("subjectId") Long subjectId, 
            @Param("subjectType") String subjectType,
            @Param("limit") int limit);
    
    @Select("SELECT COUNT(*) FROM credit_record WHERE subject_id = #{subjectId} AND subject_type = #{subjectType} AND DATE(create_time) = CURDATE() AND del_token = '0'")
    long countTodayBySubject(
            @Param("subjectId") Long subjectId, 
            @Param("subjectType") String subjectType);
    
    @Select("SELECT SUM(change_amount) FROM credit_record WHERE subject_id = #{subjectId} AND subject_type = #{subjectType} AND DATE(create_time) = CURDATE() AND del_token = '0'")
    Integer sumTodayChangeBySubject(
            @Param("subjectId") Long subjectId, 
            @Param("subjectType") String subjectType);
}
