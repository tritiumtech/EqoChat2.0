package com.eqochat.business.world.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.world.entity.WorldPostUpvote;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WorldPostUpvoteMapper extends BaseMapper<WorldPostUpvote> {

    @Select("""
            SELECT * FROM world_post_upvote
            WHERE post_id = #{postId}
              AND voter_id = #{voterId}
              AND voter_type = #{voterType}
              AND del_token = '0'
            LIMIT 1
            """)
    WorldPostUpvote findActive(@Param("postId") Long postId,
                               @Param("voterId") Long voterId,
                               @Param("voterType") String voterType);

    @Select("""
            SELECT * FROM world_post_upvote
            WHERE post_id = #{postId}
              AND voter_id = #{voterId}
              AND voter_type = #{voterType}
            LIMIT 1
            """)
    WorldPostUpvote findAny(@Param("postId") Long postId,
                            @Param("voterId") Long voterId,
                            @Param("voterType") String voterType);
}
