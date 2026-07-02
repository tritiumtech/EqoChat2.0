package com.eqochat.business.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.contact.entity.ContactRelationship;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ContactRelationshipMapper extends BaseMapper<ContactRelationship> {

    @Select("SELECT * FROM contact_relationship WHERE user_id = #{ownerId} AND user_type = #{ownerType} AND del_token = '0'")
    List<ContactRelationship> findByOwner(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") ContactRelationship.RelationshipSubjectType ownerType);

    @Select("SELECT * FROM contact_relationship WHERE friend_id = #{targetId} AND friend_type = #{targetType} AND del_token = '0'")
    List<ContactRelationship> findByTarget(
            @Param("targetId") Long targetId,
            @Param("targetType") ContactRelationship.RelationshipSubjectType targetType);

    @Select("SELECT * FROM contact_relationship WHERE user_id = #{ownerId} AND user_type = #{ownerType} AND friend_id = #{targetId} AND friend_type = #{targetType} AND del_token = '0' LIMIT 1")
    Optional<ContactRelationship> findByOwnerAndTarget(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") ContactRelationship.RelationshipSubjectType ownerType,
            @Param("targetId") Long targetId,
            @Param("targetType") ContactRelationship.RelationshipSubjectType targetType);

    @Select("SELECT * FROM contact_relationship WHERE user_id = #{ownerId} AND user_type = #{ownerType} AND status = 'ACTIVE' AND del_token = '0'")
    List<ContactRelationship> findActiveFriendsByOwner(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") ContactRelationship.RelationshipSubjectType ownerType);

    @Select("SELECT COUNT(*) FROM contact_relationship WHERE user_id = #{ownerId} AND user_type = #{ownerType} AND status = 'ACTIVE' AND del_token = '0'")
    long countActiveFriends(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") ContactRelationship.RelationshipSubjectType ownerType);

    @Select("SELECT EXISTS(SELECT 1 FROM contact_relationship WHERE user_id = #{ownerId} AND user_type = #{ownerType} AND friend_id = #{targetId} AND friend_type = #{targetType} AND status = 'ACTIVE' AND del_token = '0')")
    boolean areFriends(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") ContactRelationship.RelationshipSubjectType ownerType,
            @Param("targetId") Long targetId,
            @Param("targetType") ContactRelationship.RelationshipSubjectType targetType);
}
