package com.eqochat.business.world.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.world.entity.WorldPostMention;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WorldPostMentionMapper extends BaseMapper<WorldPostMention> {

    @Select("""
            SELECT mentioned_user_id
            FROM world_post_mention
            WHERE post_id = #{postId}
              AND del_token = '0'
            ORDER BY id ASC
            """)
    List<Long> selectMentionedUserIdsByPostId(@Param("postId") Long postId);
}
