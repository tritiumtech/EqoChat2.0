package com.eqochat.business.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.user.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
    
    @Select("SELECT * FROM user_profile WHERE did = #{did} LIMIT 1")
    Optional<UserProfile> findByDid(@Param("did") String did);
    
    @Select("SELECT * FROM user_profile WHERE phone = #{phone} LIMIT 1")
    Optional<UserProfile> findByPhone(@Param("phone") String phone);
    
    @Select("SELECT * FROM user_profile WHERE email = #{email} LIMIT 1")
    Optional<UserProfile> findByEmail(@Param("email") String email);
    
    @Select("SELECT EXISTS(SELECT 1 FROM user_profile WHERE did = #{did})")
    boolean existsByDid(@Param("did") String did);
    
    @Select("SELECT EXISTS(SELECT 1 FROM user_profile WHERE phone = #{phone})")
    boolean existsByPhone(@Param("phone") String phone);
}
