package com.eqochat.business.world.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.world.entity.WorldPost;
import lombok.Data;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WorldPostMapper extends BaseMapper<WorldPost> {

    @Select("""
            SELECT COUNT(*) FROM world_post
            WHERE author_id = #{authorId} AND del_token = '0'
            """)
    long countByAuthorId(@Param("authorId") Long authorId);

    @Select("""
            <script>
            SELECT p.*,
                   u.nickname AS author_name,
                   u.avatar_url AS author_avatar_url,
                   (CASE WHEN uf.user_id IS NULL THEN 0 ELSE 1 END) AS is_friend,
                   (CASE WHEN up.user_id IS NULL THEN 0 ELSE 1 END) AS is_upvoted
            FROM world_post p
            JOIN user_profile u ON u.id = p.author_id
            LEFT JOIN user_friend uf
              ON uf.user_id = #{viewerId}
             AND uf.friend_id = p.author_id
             AND uf.del_token = '0'
             AND uf.status = 'ACTIVE'
            LEFT JOIN world_post_upvote up
              ON up.post_id = p.id
             AND up.user_id = #{viewerId}
             AND up.del_token = '0'
            WHERE p.del_token = '0'
              <if test="cursorId != null">
                AND p.id &lt; #{cursorId}
              </if>
            ORDER BY
              <choose>
                <when test="sortBy == 'upvotes'">p.upvote_count DESC, p.id DESC</when>
                <when test="sortBy == 'topics'">
                  (
                    CASE WHEN EXISTS (
                      SELECT 1
                      FROM world_post_topic pt
                      JOIN world_topic_follow tf
                        ON tf.topic_id = pt.topic_id
                       AND tf.user_id = #{viewerId}
                       AND tf.del_token = '0'
                      WHERE pt.post_id = p.id
                        AND pt.del_token = '0'
                    ) THEN 1 ELSE 0 END
                  ) DESC,
                  p.upvote_count DESC,
                  p.id DESC
                </when>
                <otherwise>
                  (CASE WHEN uf.user_id IS NULL THEN 0 ELSE 1 END) DESC, p.id DESC
                </otherwise>
              </choose>
            LIMIT #{limit}
            </script>
            """)
    List<WorldPostRow> selectFeed(@Param("viewerId") Long viewerId,
                                 @Param("sortBy") String sortBy,
                                 @Param("cursorId") Long cursorId,
                                 @Param("limit") Integer limit);

    /**
     * 指定用户发布的动态（按 id 倒序），用于联系人详情「最近动态」等。
     */
    @Select("""
            <script>
            SELECT p.*,
                   u.nickname AS author_name,
                   u.avatar_url AS author_avatar_url,
                   (CASE WHEN uf.user_id IS NULL THEN 0 ELSE 1 END) AS is_friend,
                   (CASE WHEN up.user_id IS NULL THEN 0 ELSE 1 END) AS is_upvoted
            FROM world_post p
            JOIN user_profile u ON u.id = p.author_id
            LEFT JOIN user_friend uf
              ON uf.user_id = #{viewerId}
             AND uf.friend_id = p.author_id
             AND uf.del_token = '0'
             AND uf.status = 'ACTIVE'
            LEFT JOIN world_post_upvote up
              ON up.post_id = p.id
             AND up.user_id = #{viewerId}
             AND up.del_token = '0'
            WHERE p.del_token = '0'
              AND p.author_id = #{authorId}
              <if test="cursorId != null">
                AND p.id &lt; #{cursorId}
              </if>
            ORDER BY p.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<WorldPostRow> selectPostsByAuthor(@Param("viewerId") Long viewerId,
                                          @Param("authorId") Long authorId,
                                          @Param("cursorId") Long cursorId,
                                          @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT p.*,
                   u.nickname AS author_name,
                   u.avatar_url AS author_avatar_url,
                   (CASE WHEN uf.user_id IS NULL THEN 0 ELSE 1 END) AS is_friend,
                   (CASE WHEN up.user_id IS NULL THEN 0 ELSE 1 END) AS is_upvoted
            FROM world_post p
            JOIN world_post_topic pt ON pt.post_id = p.id AND pt.del_token = '0'
            JOIN world_topic t ON t.id = pt.topic_id AND t.del_token = '0' AND t.name = #{topicName}
            JOIN user_profile u ON u.id = p.author_id
            LEFT JOIN user_friend uf
              ON uf.user_id = #{viewerId}
             AND uf.friend_id = p.author_id
             AND uf.del_token = '0'
             AND uf.status = 'ACTIVE'
            LEFT JOIN world_post_upvote up
              ON up.post_id = p.id
             AND up.user_id = #{viewerId}
             AND up.del_token = '0'
            WHERE p.del_token = '0'
              <if test="cursorId != null">
                AND p.id &lt; #{cursorId}
              </if>
            ORDER BY p.upvote_count DESC, p.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<WorldPostRow> selectTopicPosts(@Param("viewerId") Long viewerId,
                                       @Param("topicName") String topicName,
                                       @Param("cursorId") Long cursorId,
                                       @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT p.*,
                   u.nickname AS author_name,
                   u.avatar_url AS author_avatar_url,
                   (CASE WHEN uf.user_id IS NULL THEN 0 ELSE 1 END) AS is_friend,
                   (CASE WHEN up.user_id IS NULL THEN 0 ELSE 1 END) AS is_upvoted
            FROM world_post_mention m
            JOIN world_post p
              ON p.id = m.post_id
             AND p.del_token = '0'
            JOIN user_profile u ON u.id = p.author_id
            LEFT JOIN user_friend uf
              ON uf.user_id = #{viewerId}
             AND uf.friend_id = p.author_id
             AND uf.del_token = '0'
             AND uf.status = 'ACTIVE'
            LEFT JOIN world_post_upvote up
              ON up.post_id = p.id
             AND up.user_id = #{viewerId}
             AND up.del_token = '0'
            WHERE m.mentioned_user_id = #{viewerId}
              AND m.del_token = '0'
              <if test="cursorId != null">
                AND p.id &lt; #{cursorId}
              </if>
            ORDER BY p.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<WorldPostRow> selectMentionFeed(@Param("viewerId") Long viewerId,
                                         @Param("cursorId") Long cursorId,
                                         @Param("limit") Integer limit);

    /**
     * 当前用户自己发布的动态（用于 My Tab 时间线展示）。
     */
    @Select("""
            <script>
            SELECT p.*,
                   u.nickname AS author_name,
                   u.avatar_url AS author_avatar_url,
                   (CASE WHEN uf.user_id IS NULL THEN 0 ELSE 1 END) AS is_friend,
                   (CASE WHEN up.user_id IS NULL THEN 0 ELSE 1 END) AS is_upvoted
            FROM world_post p
            JOIN user_profile u ON u.id = p.author_id
            LEFT JOIN user_friend uf
              ON uf.user_id = #{viewerId}
             AND uf.friend_id = p.author_id
             AND uf.del_token = '0'
             AND uf.status = 'ACTIVE'
            LEFT JOIN world_post_upvote up
              ON up.post_id = p.id
             AND up.user_id = #{viewerId}
             AND up.del_token = '0'
            WHERE p.del_token = '0'
              AND p.author_id = #{viewerId}
              <if test="cursorId != null">
                AND p.id &lt; #{cursorId}
              </if>
            ORDER BY p.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<WorldPostRow> selectMyPosts(@Param("viewerId") Long viewerId,
                                     @Param("cursorId") Long cursorId,
                                     @Param("limit") Integer limit);

    @Select("""
            SELECT t.name
            FROM world_post_topic pt
            JOIN world_topic t ON t.id = pt.topic_id
            WHERE pt.post_id = #{postId}
              AND pt.del_token = '0'
              AND t.del_token = '0'
            ORDER BY t.follower_count DESC, t.id DESC
            """)
    List<String> selectTopicNamesByPostId(@Param("postId") Long postId);

    @Data
    class WorldPostRow {
        private Long id;
        private Long authorId;
        private String content;
        private String mediaType;
        private String imageUrl;
        private String videoUrl;
        private Integer replyCount;
        private Integer upvoteCount;
        private LocalDateTime createTime;
        private String authorName;
        private String authorAvatarUrl;
        private Integer isFriend;
        private Integer isUpvoted;
    }
}

