package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.AgentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AgentProfileMapper extends BaseMapper<AgentProfile> {
    
    @Select("SELECT * FROM agent_profile WHERE did = #{did} LIMIT 1")
    Optional<AgentProfile> findByDid(@Param("did") String did);
    
    @Select("SELECT * FROM agent_profile WHERE owner_id = #{ownerId} AND status = 'ACTIVE'")
    List<AgentProfile> findActiveByOwnerId(@Param("ownerId") Long ownerId);
    
    @Select("SELECT COUNT(*) FROM agent_profile WHERE owner_id = #{ownerId} AND status = 'ACTIVE'")
    Long countActiveByOwnerId(@Param("ownerId") Long ownerId);
}
