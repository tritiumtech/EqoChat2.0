package com.eqochat.business.credit.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 跨 user/agent 表只读查询信用分，避免 credit 模块依赖 user、agent 实现而产生 Maven 循环依赖。
 */
@Mapper
public interface SubjectCreditScoreMapper {

    @Select("SELECT credit_score FROM user_profile WHERE id = #{id}")
    Integer selectUserCreditScoreById(@Param("id") Long id);

    @Select("SELECT credit_score FROM agent_profile WHERE id = #{id}")
    Integer selectAgentCreditScoreById(@Param("id") Long id);
}
