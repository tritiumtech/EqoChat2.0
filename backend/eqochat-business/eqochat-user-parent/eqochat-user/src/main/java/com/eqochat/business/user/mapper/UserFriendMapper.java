package com.eqochat.business.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.user.entity.UserFriend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserFriendMapper extends BaseMapper<UserFriend> {
    
    @Select("SELECT * FROM user_friend WHERE user_id = #{ownerId} AND user_type = #{ownerType} AND del_token = '0'")
    List<UserFriend> findByOwner(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") UserFriend.FriendType ownerType);
    
    @Select("SELECT * FROM user_friend WHERE friend_id = #{targetId} AND friend_type = #{targetType} AND del_token = '0'")
    List<UserFriend> findByTarget(
            @Param("targetId") Long targetId,
            @Param("targetType") UserFriend.FriendType targetType);
    
    @Select("SELECT * FROM user_friend WHERE user_id = #{ownerId} AND user_type = #{ownerType} AND friend_id = #{targetId} AND friend_type = #{targetType} AND del_token = '0' LIMIT 1")
    Optional<UserFriend> findByOwnerAndTarget(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") UserFriend.FriendType ownerType,
            @Param("targetId") Long targetId,
            @Param("targetType") UserFriend.FriendType targetType);
    
    @Select("SELECT * FROM user_friend WHERE user_id = #{ownerId} AND user_type = #{ownerType} AND status = 'ACTIVE' AND del_token = '0'")
    List<UserFriend> findActiveFriendsByOwner(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") UserFriend.FriendType ownerType);
    
    @Select("SELECT COUNT(*) FROM user_friend WHERE user_id = #{ownerId} AND user_type = #{ownerType} AND status = 'ACTIVE' AND del_token = '0'")
    long countActiveFriends(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") UserFriend.FriendType ownerType);
    
    @Select("SELECT EXISTS(SELECT 1 FROM user_friend WHERE user_id = #{ownerId} AND user_type = #{ownerType} AND friend_id = #{targetId} AND friend_type = #{targetType} AND status = 'ACTIVE' AND del_token = '0')")
    boolean areFriends(
            @Param("ownerId") Long ownerId,
            @Param("ownerType") UserFriend.FriendType ownerType,
            @Param("targetId") Long targetId,
            @Param("targetType") UserFriend.FriendType targetType);
}
