package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.UserAuthRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAuthRecordMapper extends BaseMapper<UserAuthRecord> {
    
    @Select("SELECT * FROM user_auth_record WHERE user_id = #{userId} AND del_token = '0'")
    List<UserAuthRecord> findByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM user_auth_record WHERE user_id = #{userId} AND auth_type = #{authType} AND del_token = '0' LIMIT 1")
    UserAuthRecord findByUserIdAndType(@Param("userId") Long userId, @Param("authType") String authType);
    
    @Select("SELECT * FROM user_auth_record WHERE auth_identifier = #{identifier} AND del_token = '0' LIMIT 1")
    UserAuthRecord findByAuthIdentifier(@Param("identifier") String identifier);
}
