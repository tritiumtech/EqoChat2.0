package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.AgentBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AgentBindingMapper extends BaseMapper<AgentBinding> {
    
    @Select("SELECT * FROM agent_binding WHERE agent_id = #{agentId} AND del_token = '0'")
    List<AgentBinding> findByAgentId(@Param("agentId") Long agentId);
    
    @Select("SELECT * FROM agent_binding WHERE owner_id = #{ownerId} AND del_token = '0'")
    List<AgentBinding> findByOwnerId(@Param("ownerId") Long ownerId);
    
    @Select("SELECT * FROM agent_binding WHERE agent_id = #{agentId} AND owner_id = #{ownerId} AND del_token = '0' LIMIT 1")
    Optional<AgentBinding> findByAgentIdAndOwnerId(@Param("agentId") Long agentId, @Param("ownerId") Long ownerId);
    
    @Select("SELECT EXISTS(SELECT 1 FROM agent_binding WHERE agent_id = #{agentId} AND owner_id = #{ownerId} AND del_token = '0')")
    boolean existsBinding(@Param("agentId") Long agentId, @Param("ownerId") Long ownerId);
}
