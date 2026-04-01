package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.WorldPostReplyUpvote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorldPostReplyUpvoteMapper extends BaseMapper<WorldPostReplyUpvote> {

    @Select("""
            SELECT * FROM world_post_reply_upvote
            WHERE reply_id = #{replyId}
              AND user_id = #{userId}
              AND del_token = '0'
            LIMIT 1
            """)
    WorldPostReplyUpvote findActive(@Param("replyId") Long replyId, @Param("userId") Long userId);

    @Select("""
            SELECT * FROM world_post_reply_upvote
            WHERE reply_id = #{replyId}
              AND user_id = #{userId}
            LIMIT 1
            """)
    WorldPostReplyUpvote findAny(@Param("replyId") Long replyId, @Param("userId") Long userId);
}

