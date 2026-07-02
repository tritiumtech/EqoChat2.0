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
                        THEN COALESCE(sr.display_name, CONCAT('AGENT:', p.author_id))
                        ELSE COALESCE(sr.display_name, CONCAT('HUMAN:', p.author_id))
                   END AS author_name,
                   sr.avatar_url AS author_avatar_url,
                   sr.associated_human_id AS author_owner_id,
                   COALESCE(sr.associated_human_name, owner_sr.display_name) AS author_owner_name,
                   0 AS is_friend,
                   (CASE WHEN up.voter_id IS NULL THEN 0 ELSE 1 END) AS is_upvoted
            """;

    String AUTHOR_IDENTITY_JOINS = """
            LEFT JOIN subject_registry sr
              ON sr.subject_id = p.author_id
             AND sr.subject_type = p.author_type
             AND sr.del_token = '0'
            LEFT JOIN subject_registry owner_sr
              ON p.author_type = 'AGENT'
             AND owner_sr.subject_id = sr.associated_human_id
             AND owner_sr.subject_type = 'HUMAN'
             AND owner_sr.del_token = '0'
            LEFT JOIN world_post_upvote up
              ON up.post_id = p.id
             AND up.voter_id = #{viewerId}
             AND up.voter_type = #{viewerType}
             AND up.del_token = '0'
            """;

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
                       AND tf.follower_type = #{viewerType}
                       AND tf.del_token = '0'
                      WHERE pt.post_id = p.id
                        AND pt.del_token = '0'
                    ) THEN 1 ELSE 0 END
                  ) DESC,
                  p.upvote_count DESC,
                  p.id DESC
                </when>
                <otherwise>
                  (
                    CASE
                      <if test="friendHumanIds != null and friendHumanIds.size() > 0">
                        WHEN p.author_type = 'HUMAN' AND p.author_id IN
                        <foreach collection="friendHumanIds" item="id" open="(" separator="," close=")">
                          #{id}
                        </foreach>
                        THEN 1
                      </if>
                      <if test="friendAgentIds != null and friendAgentIds.size() > 0">
                        WHEN p.author_type = 'AGENT' AND p.author_id IN
                        <foreach collection="friendAgentIds" item="id" open="(" separator="," close=")">
                          #{id}
                        </foreach>
                        THEN 1
                      </if>
                      ELSE 0
                    END
                  ) DESC, p.id DESC
                </otherwise>
              </choose>
            LIMIT #{limit}
            </script>
            """)
    List<WorldPostRow> selectFeed(@Param("viewerId") Long viewerId,
                                  @Param("viewerType") String viewerType,
                                  @Param("sortBy") String sortBy,
                                  @Param("friendHumanIds") List<Long> friendHumanIds,
                                  @Param("friendAgentIds") List<Long> friendAgentIds,
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
                                           @Param("viewerType") String viewerType,
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
                                        @Param("viewerType") String viewerType,
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
              AND m.mentioned_subject_type = #{viewerType}
              AND m.del_token = '0'
              <if test="cursorId != null">
                AND p.id &lt; #{cursorId}
              </if>
            ORDER BY p.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<WorldPostRow> selectMentionFeed(@Param("viewerId") Long viewerId,
                                         @Param("viewerType") String viewerType,
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
              AND p.author_type = #{viewerType}
              <if test="cursorId != null">
                AND p.id &lt; #{cursorId}
              </if>
            ORDER BY p.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<WorldPostRow> selectMyPosts(@Param("viewerId") Long viewerId,
                                     @Param("viewerType") String viewerType,
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
        private Long authorOwnerId;
        private String authorOwnerName;
        private Integer isFriend;
        private Integer isUpvoted;
    }
}
