package com.eqochat.business.world.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.world.entity.WorldTopicFollow;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WorldTopicFollowMapper extends BaseMapper<WorldTopicFollow> {

    @Select("""
            SELECT * FROM world_topic_follow
            WHERE topic_id = #{topicId}
              AND follower_id = #{followerId}
              AND follower_type = #{followerType}
              AND del_token = '0'
            LIMIT 1
            """)
    WorldTopicFollow findActive(@Param("topicId") Long topicId,
                                @Param("followerId") Long followerId,
                                @Param("followerType") String followerType);

    @Select("""
            SELECT * FROM world_topic_follow
            WHERE topic_id = #{topicId}
              AND follower_id = #{followerId}
              AND follower_type = #{followerType}
            LIMIT 1
            """)
    WorldTopicFollow findAny(@Param("topicId") Long topicId,
                             @Param("followerId") Long followerId,
                             @Param("followerType") String followerType);
}
