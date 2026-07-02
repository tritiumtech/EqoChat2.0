package com.eqochat.business.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
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
    
    default List<GroupProfile> findByOwner(SubjectRef owner) {
        return owner == null ? List.of() : findByOwner(owner.id(), owner.type());
    }

    @Select("SELECT * FROM group_profile WHERE owner_id = #{ownerId} AND owner_type = #{ownerType} AND del_token = '0'")
    List<GroupProfile> findByOwner(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") SubjectType ownerType
    );
    
    @Select("SELECT * FROM group_profile WHERE status = 'ACTIVE' AND del_token = '0'")
    List<GroupProfile> findAllActive();
    
    @Select("SELECT * FROM group_profile WHERE group_name LIKE CONCAT('%', #{keyword}, '%') AND del_token = '0'")
    List<GroupProfile> searchByName(@Param("keyword") String keyword);
}
