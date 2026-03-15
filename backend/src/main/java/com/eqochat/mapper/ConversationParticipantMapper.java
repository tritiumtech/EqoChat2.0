package com.eqochat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.domain.entity.ConversationParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ConversationParticipantMapper extends BaseMapper<ConversationParticipant> {
    
    @Select("SELECT * FROM conversation_participant WHERE conversation_id = #{conversationId} AND del_token = '0'")
    List<ConversationParticipant> findByConversationId(@Param("conversationId") Long conversationId);
    
    @Select("SELECT * FROM conversation_participant WHERE participant_id = #{participantId} AND del_token = '0'")
    List<ConversationParticipant> findByParticipantId(@Param("participantId") Long participantId);
    
    @Select("SELECT * FROM conversation_participant WHERE conversation_id = #{conversationId} AND participant_id = #{participantId} AND del_token = '0' LIMIT 1")
    Optional<ConversationParticipant> findByConversationAndParticipant(
            @Param("conversationId") Long conversationId, 
            @Param("participantId") Long participantId);
    
    @Select("SELECT COUNT(*) FROM conversation_participant WHERE conversation_id = #{conversationId} AND del_token = '0'")
    long countByConversationId(@Param("conversationId") Long conversationId);
}
