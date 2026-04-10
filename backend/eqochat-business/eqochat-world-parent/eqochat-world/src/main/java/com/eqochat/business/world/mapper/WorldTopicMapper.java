package com.eqochat.business.world.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.world.entity.WorldTopic;
import lombok.Data;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorldTopicMapper extends BaseMapper<WorldTopic> {

    @Select("""
            SELECT t.*,
                   (CASE WHEN f.user_id IS NULL THEN 0 ELSE 1 END) AS is_following
            FROM world_topic t
            LEFT JOIN world_topic_follow f
              ON f.topic_id = t.id
             AND f.user_id = #{viewerId}
             AND f.del_token = '0'
            WHERE t.del_token = '0'
            ORDER BY t.follower_count DESC, t.post_count DESC, t.id DESC
            LIMIT #{limit}
            """)
    List<WorldTopicRow> selectTopTopics(@Param("viewerId") Long viewerId, @Param("limit") Integer limit);

    @Select("""
            SELECT t.*,
                   (CASE WHEN f.user_id IS NULL THEN 0 ELSE 1 END) AS is_following
            FROM world_topic t
            LEFT JOIN world_topic_follow f
              ON f.topic_id = t.id
             AND f.user_id = #{viewerId}
             AND f.del_token = '0'
            WHERE t.del_token = '0'
              AND (#{cursorId} IS NULL OR t.id < #{cursorId})
            ORDER BY t.follower_count DESC, t.post_count DESC, t.id DESC
            LIMIT #{limit}
            """)
    List<WorldTopicRow> selectTopTopicsWithCursor(@Param("viewerId") Long viewerId, @Param("cursorId") Long cursorId, @Param("limit") Integer limit);

    @Select("""
            SELECT t.*
            FROM world_topic t
            WHERE t.del_token = '0' AND t.name = #{name}
            LIMIT 1
            """)
    WorldTopic selectByName(@Param("name") String name);

    @Data
    class WorldTopicRow {
        private Long id;
        private String name;
        private Integer postCount;
        private Integer followerCount;
        private Integer isFollowing;
    }
}

