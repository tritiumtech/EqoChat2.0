package com.eqochat.business.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.contact.entity.ContactRelationship;
import com.eqochat.business.contact.entity.ContactTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContactTagMapper extends BaseMapper<ContactTag> {

    @Select("SELECT tag_name FROM contact_tag WHERE user_id = #{userId} AND user_type = #{userType} AND friend_id = #{friendId} AND friend_type = #{friendType} AND del_token = '0' ORDER BY id ASC")
    List<String> selectActiveTagNames(
            @Param("userId") Long userId,
            @Param("userType") ContactRelationship.RelationshipSubjectType userType,
            @Param("friendId") Long friendId,
            @Param("friendType") ContactRelationship.RelationshipSubjectType friendType);

    @Delete("DELETE FROM contact_tag WHERE user_id = #{userId} AND user_type = #{userType} AND friend_id = #{friendId} AND friend_type = #{friendType}")
    int hardDeleteAll(
            @Param("userId") Long userId,
            @Param("userType") ContactRelationship.RelationshipSubjectType userType,
            @Param("friendId") Long friendId,
            @Param("friendType") ContactRelationship.RelationshipSubjectType friendType);
}
