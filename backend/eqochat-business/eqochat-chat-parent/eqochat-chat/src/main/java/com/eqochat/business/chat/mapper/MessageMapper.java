package com.eqochat.business.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.chat.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    
    @Select("SELECT * FROM message WHERE conversation_id = #{conversationId} AND del_token = '0' ORDER BY id DESC LIMIT #{limit}")
    List<Message> selectByConversationId(@Param("conversationId") Long conversationId, @Param("limit") Integer limit);
    
    @Select("SELECT * FROM message WHERE conversation_id = #{conversationId} AND id < #{lastId} AND del_token = '0' ORDER BY id DESC LIMIT #{limit}")
    List<Message> selectByConversationIdWithCursor(@Param("conversationId") Long conversationId, 
                                                      @Param("lastId") Long lastId, 
                                                      @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM message WHERE conversation_id = #{conversationId} AND del_token = '0'")
    long countByConversationId(@Param("conversationId") Long conversationId);

    @Select("SELECT COUNT(*) FROM message WHERE conversation_id = #{conversationId} AND id < #{lastId} AND del_token = '0'")
    long countOlderMessages(@Param("conversationId") Long conversationId, @Param("lastId") Long lastId);

    @Select("""
            <script>
            SELECT COUNT(*) FROM message
            WHERE conversation_id = #{conversationId}
              AND del_token = '0'
              AND NOT (sender_id = #{readerId} AND sender_type = #{readerType})
              <if test="lastReadMessageId != null">
                AND id &gt; #{lastReadMessageId}
              </if>
            </script>
            """)
    long countUnreadMessages(@Param("conversationId") Long conversationId,
                             @Param("readerId") Long readerId,
                             @Param("readerType") SubjectType readerType,
                             @Param("lastReadMessageId") Long lastReadMessageId);
}
