package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {
    
    @Select("SELECT * FROM system_config WHERE config_key = #{key} AND del_token = '0' LIMIT 1")
    Optional<SystemConfig> findByKey(@Param("key") String key);
    
    @Select("SELECT config_value FROM system_config WHERE config_key = #{key} AND del_token = '0' LIMIT 1")
    String getValueByKey(@Param("key") String key);
    
    @Select("SELECT * FROM system_config WHERE del_token = '0'")
    List<SystemConfig> findAllActive();
    
    @Select("SELECT EXISTS(SELECT 1 FROM system_config WHERE config_key = #{key} AND del_token = '0')")
    boolean existsByKey(@Param("key") String key);
}
