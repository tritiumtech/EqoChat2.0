package com.eqochat.business.contact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.contact.entity.UserContactTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserContactTagMapper extends BaseMapper<UserContactTag> {

    @Select("SELECT tag_name FROM user_contact_tag WHERE user_id = #{userId} AND friend_id = #{friendId} AND del_token = '0' ORDER BY id ASC")
    List<String> selectActiveTagNames(@Param("userId") Long userId, @Param("friendId") Long friendId);

    @Delete("DELETE FROM user_contact_tag WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int hardDeleteAll(@Param("userId") Long userId, @Param("friendId") Long friendId);
}
