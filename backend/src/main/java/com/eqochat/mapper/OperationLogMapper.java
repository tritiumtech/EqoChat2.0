package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
    
    @Select("SELECT * FROM operation_log WHERE operator_id = #{operatorId} AND del_token = '0' ORDER BY create_time DESC")
    List<OperationLog> findByOperatorId(@Param("operatorId") Long operatorId);
    
    @Select("SELECT * FROM operation_log WHERE operator_id = #{operatorId} AND operator_type = #{operatorType} AND del_token = '0' ORDER BY create_time DESC")
    List<OperationLog> findByOperator(
            @Param("operatorId") Long operatorId, 
            @Param("operatorType") String operatorType);
    
    @Select("SELECT * FROM operation_log WHERE operation_type = #{operationType} AND del_token = '0' ORDER BY create_time DESC")
    List<OperationLog> findByOperationType(@Param("operationType") String operationType);
    
    @Select("SELECT * FROM operation_log WHERE target_type = #{targetType} AND target_id = #{targetId} AND del_token = '0' ORDER BY create_time DESC")
    List<OperationLog> findByTarget(
            @Param("targetType") String targetType, 
            @Param("targetId") Long targetId);
    
    @Select("SELECT * FROM operation_log WHERE log_date = #{date} AND del_token = '0' ORDER BY create_time DESC")
    List<OperationLog> findByLogDate(@Param("date") java.sql.Date date);
    
    @Select("SELECT * FROM operation_log WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR) AND del_token = '0' ORDER BY create_time DESC")
    List<OperationLog> findRecent(@Param("hours") int hours);
}
