package com.eqochat.business.world.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.world.entity.WorldPostReplyUpvote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorldPostReplyUpvoteMapper extends BaseMapper<WorldPostReplyUpvote> {

    @Select("""
            SELECT * FROM world_post_reply_upvote
            WHERE reply_id = #{replyId}
              AND voter_id = #{voterId}
              AND voter_type = #{voterType}
              AND del_token = '0'
            LIMIT 1
            """)
    WorldPostReplyUpvote findActive(@Param("replyId") Long replyId,
                                    @Param("voterId") Long voterId,
                                    @Param("voterType") String voterType);

    @Select("""
            SELECT * FROM world_post_reply_upvote
            WHERE reply_id = #{replyId}
              AND voter_id = #{voterId}
              AND voter_type = #{voterType}
            LIMIT 1
            """)
    WorldPostReplyUpvote findAny(@Param("replyId") Long replyId,
                                 @Param("voterId") Long voterId,
                                 @Param("voterType") String voterType);
}
