package com.eqochat.business.world.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.world.entity.WorldPost;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WorldPostMapper extends BaseMapper<WorldPost> {

    String POST_ROW_COLUMNS = """
                   p.*,
                   p.author_type AS author_type,
                   CASE WHEN p.author_type = 'AGENT'
                        THEN COALESCE(ap.name, 'Agent')
                        ELSE COALESCE(u.nickname, 'User')
                   END AS author_name,
                   CASE WHEN p.author_type = 'AGENT'
                        THEN ap.avatar_url
                        ELSE u.avatar_url
                   END AS author_avatar_url,
                   (CASE WHEN p.author_type = 'AGENT' THEN 1 ELSE 0 END) AS author_ai,
                   ap.owner_id AS author_owner_id,
                   owner.nickname AS author_owner_name,
                   (CASE WHEN uf.user_id IS NULL THEN 0 ELSE 1 END) AS is_friend,
                   (CASE WHEN up.voter_id IS NULL THEN 0 ELSE 1 END) AS is_upvoted
            """;

    String AUTHOR_IDENTITY_JOINS = """
            LEFT JOIN user_profile u
              ON p.author_type = 'HUMAN'
             AND u.id = p.author_id
             AND u.del_token = '0'
            LEFT JOIN agent_profile ap
              ON p.author_type = 'AGENT'
             AND ap.id = p.author_id
             AND ap.del_token = '0'
             AND ap.status = 'ACTIVE'
            LEFT JOIN user_profile owner
              ON p.author_type = 'AGENT'
             AND owner.id = ap.owner_id
            LEFT JOIN user_friend uf
              ON uf.user_id = #{viewerId}
             AND uf.user_type = 'HUMAN'
             AND uf.friend_id = p.author_id
             AND uf.friend_type = p.author_type
             AND uf.del_token = '0'
             AND uf.status = 'ACTIVE'
            LEFT JOIN world_post_upvote up
              ON up.post_id = p.id
             AND up.voter_id = #{viewerId}
             AND up.voter_type = 'HUMAN'
             AND up.del_token = '0'
            """;

    @Select("""
            SELECT COUNT(*) FROM world_post
            WHERE author_id = #{authorId}
              AND author_type = 'HUMAN'
              AND del_token = '0'
            """)
    long countByAuthorId(@Param("authorId") Long authorId);

    @Select("""
            SELECT COUNT(*) FROM world_post
            WHERE author_id = #{authorId}
              AND author_type = #{authorType}
              AND del_token = '0'
            """)
    long countByAuthor(@Param("authorId") Long authorId, @Param("authorType") String authorType);

    @Select("""
            <script>
            SELECT
            """ + POST_ROW_COLUMNS + """
            FROM world_post p
            """ + AUTHOR_IDENTITY_JOINS + """
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
                       AND tf.follower_id = #{viewerId}
                       AND tf.follower_type = 'HUMAN'
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
     * 指定主体发布的动态（按 id 倒序），用于联系人详情「最近动态」等。
     */
    @Select("""
            <script>
            SELECT
            """ + POST_ROW_COLUMNS + """
            FROM world_post p
            """ + AUTHOR_IDENTITY_JOINS + """
            WHERE p.del_token = '0'
              AND p.author_id = #{authorId}
              AND p.author_type = #{authorType}
              <if test="cursorId != null">
                AND p.id &lt; #{cursorId}
              </if>
            ORDER BY p.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<WorldPostRow> selectPostsByAuthor(@Param("viewerId") Long viewerId,
                                           @Param("authorId") Long authorId,
                                           @Param("authorType") String authorType,
                                           @Param("cursorId") Long cursorId,
                                           @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT
            """ + POST_ROW_COLUMNS + """
            FROM world_post p
            JOIN world_post_topic pt ON pt.post_id = p.id AND pt.del_token = '0'
            JOIN world_topic t ON t.id = pt.topic_id AND t.del_token = '0' AND t.name = #{topicName}
            """ + AUTHOR_IDENTITY_JOINS + """
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
            SELECT
            """ + POST_ROW_COLUMNS + """
            FROM world_post_mention m
            JOIN world_post p
              ON p.id = m.post_id
             AND p.del_token = '0'
            """ + AUTHOR_IDENTITY_JOINS + """
            WHERE m.mentioned_subject_id = #{viewerId}
              AND m.mentioned_subject_type = 'HUMAN'
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
     * 当前人类用户自己发布的动态（用于 My Tab 时间线展示）。
     */
    @Select("""
            <script>
            SELECT
            """ + POST_ROW_COLUMNS + """
            FROM world_post p
            """ + AUTHOR_IDENTITY_JOINS + """
            WHERE p.del_token = '0'
              AND p.author_id = #{viewerId}
              AND p.author_type = 'HUMAN'
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
        private Long sharedProjectId;
        private String sharedProjectName;
        private String sharedProjectOwnerName;
        private Boolean sharedProjectOwnerAi;
        private String sharedProjectAssociatedHumanName;
        private String sharedProjectBudget;
        private String sharedProjectTeamMix;
        private String sharedProjectDeadline;
        private String sharedProjectStatus;
        private Integer replyCount;
        private Integer upvoteCount;
        private LocalDateTime createTime;
        private String authorType;
        private String authorName;
        private String authorAvatarUrl;
        private Integer authorAi;
        private Long authorOwnerId;
        private String authorOwnerName;
        private Integer isFriend;
        private Integer isUpvoted;
    }
}
