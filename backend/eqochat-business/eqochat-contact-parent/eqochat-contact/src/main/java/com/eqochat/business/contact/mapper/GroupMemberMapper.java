package com.eqochat.business.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
    
    @Select("SELECT * FROM group_member WHERE user_id = #{userId} AND del_token = '0'")
    List<GroupMember> findByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM group_member WHERE group_id = #{groupId} AND user_id = #{userId} AND del_token = '0' LIMIT 1")
    Optional<GroupMember> findByGroupAndUser(
            @Param("groupId") Long groupId, 
            @Param("userId") Long userId);
    
    @Select("SELECT * FROM group_member WHERE group_id = #{groupId} AND role = 'OWNER' AND del_token = '0' LIMIT 1")
    Optional<GroupMember> findOwnerByGroupId(@Param("groupId") Long groupId);
    
    @Select("SELECT * FROM group_member WHERE group_id = #{groupId} AND role IN ('OWNER', 'ADMIN') AND del_token = '0'")
    List<GroupMember> findAdminsByGroupId(@Param("groupId") Long groupId);
    
    @Select("SELECT COUNT(*) FROM group_member WHERE group_id = #{groupId} AND del_token = '0'")
    long countByGroupId(@Param("groupId") Long groupId);
    
    @Select("SELECT EXISTS(SELECT 1 FROM group_member WHERE group_id = #{groupId} AND user_id = #{userId} AND del_token = '0')")
    boolean isMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
