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
    
    @Select("SELECT * FROM user_friend WHERE user_id = #{userId} AND del_token = '0'")
    List<UserFriend> findByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM user_friend WHERE friend_id = #{friendId} AND del_token = '0'")
    List<UserFriend> findByFriendId(@Param("friendId") Long friendId);
    
    @Select("SELECT * FROM user_friend WHERE user_id = #{userId} AND friend_id = #{friendId} AND del_token = '0' LIMIT 1")
    Optional<UserFriend> findByUserAndFriend(
            @Param("userId") Long userId, 
            @Param("friendId") Long friendId);
    
    @Select("SELECT * FROM user_friend WHERE user_id = #{userId} AND status = 'ACTIVE' AND del_token = '0'")
    List<UserFriend> findActiveFriendsByUserId(@Param("userId") Long userId);
    
    @Select("SELECT COUNT(*) FROM user_friend WHERE user_id = #{userId} AND status = 'ACTIVE' AND del_token = '0'")
    long countActiveFriends(@Param("userId") Long userId);
    
    @Select("SELECT EXISTS(SELECT 1 FROM user_friend WHERE user_id = #{userId} AND friend_id = #{friendId} AND status = 'ACTIVE' AND del_token = '0')")
    boolean areFriends(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
