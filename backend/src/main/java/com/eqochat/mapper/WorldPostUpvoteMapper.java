package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.WorldPostUpvote;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WorldPostUpvoteMapper extends BaseMapper<WorldPostUpvote> {

    @Select("""
            SELECT * FROM world_post_upvote
            WHERE post_id = #{postId}
              AND user_id = #{userId}
              AND del_token = '0'
            LIMIT 1
            """)
    WorldPostUpvote findActive(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("""
            SELECT * FROM world_post_upvote
            WHERE post_id = #{postId}
              AND user_id = #{userId}
            LIMIT 1
            """)
    WorldPostUpvote findAny(@Param("postId") Long postId, @Param("userId") Long userId);
}

