package com.eqochat.business.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.agent.entity.AgentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AgentProfileMapper extends BaseMapper<AgentProfile> {
    
    @Select("SELECT * FROM agent_profile WHERE did = #{did} AND del_token = '0' LIMIT 1")
    Optional<AgentProfile> findByDid(@Param("did") String did);

    @Select("SELECT * FROM agent_profile WHERE name = #{name} AND del_token = '0' ORDER BY id ASC LIMIT 10")
    List<AgentProfile> findByName(@Param("name") String name);
    
    @Select("SELECT * FROM agent_profile WHERE owner_id = #{ownerId} AND status = 'ACTIVE' AND del_token = '0'")
    List<AgentProfile> findActiveByOwnerId(@Param("ownerId") Long ownerId);
    
    @Select("SELECT COUNT(*) FROM agent_profile WHERE owner_id = #{ownerId} AND status = 'ACTIVE' AND del_token = '0'")
    Long countActiveByOwnerId(@Param("ownerId") Long ownerId);
}
