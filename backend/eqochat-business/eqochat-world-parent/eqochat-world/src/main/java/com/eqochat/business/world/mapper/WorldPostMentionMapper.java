package com.eqochat.business.world.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.world.entity.WorldPostMention;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WorldPostMentionMapper extends BaseMapper<WorldPostMention> {

    @Select("""
            SELECT mentioned_subject_id,
                   mentioned_subject_type
            FROM world_post_mention
            WHERE post_id = #{postId}
              AND del_token = '0'
            ORDER BY id ASC
            """)
    List<MentionedSubjectRow> selectMentionedSubjectsByPostId(@Param("postId") Long postId);

    @Data
    class MentionedSubjectRow {
        private Long mentionedSubjectId;
        private String mentionedSubjectType;
    }
}
