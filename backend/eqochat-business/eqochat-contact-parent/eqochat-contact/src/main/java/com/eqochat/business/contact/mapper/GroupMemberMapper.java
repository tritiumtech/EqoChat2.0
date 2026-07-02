package com.eqochat.business.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.contact.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {
    
    @Select("SELECT * FROM group_member WHERE group_id = #{groupId} AND del_token = '0'")
    List<GroupMember> findByGroupId(@Param("groupId") Long groupId);
    
    default List<GroupMember> findByMember(SubjectRef member) {
        return member == null ? List.of() : findByMember(member.id(), member.type());
    }

    @Select("SELECT * FROM group_member WHERE user_id = #{memberId} AND member_type = #{memberType} AND del_token = '0'")
    List<GroupMember> findByMember(
            @Param("memberId") Long memberId,
            @Param("memberType") SubjectType memberType
    );
    
    default Optional<GroupMember> findByGroupAndMember(Long groupId, SubjectRef member) {
        return member == null ? Optional.empty() : findByGroupAndMember(groupId, member.id(), member.type());
    }

    @Select("SELECT * FROM group_member WHERE group_id = #{groupId} AND user_id = #{memberId} AND member_type = #{memberType} AND del_token = '0' LIMIT 1")
    Optional<GroupMember> findByGroupAndMember(
            @Param("groupId") Long groupId,
            @Param("memberId") Long memberId,
            @Param("memberType") SubjectType memberType
    );
    
    @Select("SELECT * FROM group_member WHERE group_id = #{groupId} AND role = 'OWNER' AND del_token = '0' LIMIT 1")
    Optional<GroupMember> findOwnerByGroupId(@Param("groupId") Long groupId);
    
    @Select("SELECT * FROM group_member WHERE group_id = #{groupId} AND role IN ('OWNER', 'ADMIN') AND del_token = '0'")
    List<GroupMember> findAdminsByGroupId(@Param("groupId") Long groupId);
    
    @Select("SELECT COUNT(*) FROM group_member WHERE group_id = #{groupId} AND del_token = '0'")
    long countByGroupId(@Param("groupId") Long groupId);
    
    default boolean isMember(Long groupId, SubjectRef member) {
        return member != null && isMember(groupId, member.id(), member.type());
    }

    @Select("SELECT EXISTS(SELECT 1 FROM group_member WHERE group_id = #{groupId} AND user_id = #{memberId} AND member_type = #{memberType} AND del_token = '0')")
    boolean isMember(
            @Param("groupId") Long groupId,
            @Param("memberId") Long memberId,
            @Param("memberType") SubjectType memberType
    );
}
