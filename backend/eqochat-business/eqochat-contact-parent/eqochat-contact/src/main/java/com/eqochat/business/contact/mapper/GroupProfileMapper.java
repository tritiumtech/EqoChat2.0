package com.eqochat.business.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.contact.entity.GroupProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface GroupProfileMapper extends BaseMapper<GroupProfile> {
    
    @Select("SELECT * FROM group_profile WHERE conversation_id = #{conversationId} AND del_token = '0' LIMIT 1")
    Optional<GroupProfile> findByConversationId(@Param("conversationId") Long conversationId);
    
    @Select("SELECT * FROM group_profile WHERE owner_id = #{ownerId} AND del_token = '0'")
    List<GroupProfile> findByOwnerId(@Param("ownerId") Long ownerId);
    
    @Select("SELECT * FROM group_profile WHERE status = 'ACTIVE' AND del_token = '0'")
    List<GroupProfile> findAllActive();
    
    @Select("SELECT * FROM group_profile WHERE group_name LIKE CONCAT('%', #{keyword}, '%') AND del_token = '0'")
    List<GroupProfile> searchByName(@Param("keyword") String keyword);
}
