package com.eqochat.business.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.contact.entity.UserContactTag;
import com.eqochat.business.user.entity.UserFriend;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserContactTagMapper extends BaseMapper<UserContactTag> {

    @Select("SELECT tag_name FROM user_contact_tag WHERE user_id = #{userId} AND user_type = #{userType} AND friend_id = #{friendId} AND friend_type = #{friendType} AND del_token = '0' ORDER BY id ASC")
    List<String> selectActiveTagNames(
            @Param("userId") Long userId,
            @Param("userType") UserFriend.FriendType userType,
            @Param("friendId") Long friendId,
            @Param("friendType") UserFriend.FriendType friendType);

    @Delete("DELETE FROM user_contact_tag WHERE user_id = #{userId} AND user_type = #{userType} AND friend_id = #{friendId} AND friend_type = #{friendType}")
    int hardDeleteAll(
            @Param("userId") Long userId,
            @Param("userType") UserFriend.FriendType userType,
            @Param("friendId") Long friendId,
            @Param("friendType") UserFriend.FriendType friendType);
}
