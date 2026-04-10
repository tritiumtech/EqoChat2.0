package com.eqochat.business.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eqochat.business.chat.entity.MessageReaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageReactionMapper extends BaseMapper<MessageReaction> {
    
    @Select("SELECT * FROM message_reaction WHERE message_id = #{messageId} AND del_token = '0'")
    List<MessageReaction> findByMessageId(@Param("messageId") Long messageId);
    
    @Select("SELECT * FROM message_reaction WHERE reactor_id = #{reactorId} AND del_token = '0'")
    List<MessageReaction> findByReactorId(@Param("reactorId") Long reactorId);
    
    @Select("SELECT * FROM message_reaction WHERE message_id = #{messageId} AND reactor_id = #{reactorId} AND del_token = '0'")
    List<MessageReaction> findByMessageAndReactor(
            @Param("messageId") Long messageId, 
            @Param("reactorId") Long reactorId);
    
    @Select("SELECT COUNT(*) FROM message_reaction WHERE message_id = #{messageId} AND del_token = '0'")
    long countByMessageId(@Param("messageId") Long messageId);
}
